package com.rental.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 风险定级规则引擎单元测试：覆盖 R1 违约金 / R2 押金 / R3 强制高风险 / R4 正面条款降级与偏好档位。
 */
class RiskRuleEngineTest {

    private RiskRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RiskRuleEngine();
    }

    /** 构造一条 AI 审查记录 */
    private Map<String, Object> record(String clause, String riskLevel, boolean isHighRisk) {
        Map<String, Object> item = new HashMap<>();
        item.put("clauseContent", clause);
        item.put("riskType", "测试类型");
        item.put("riskLevel", riskLevel);
        item.put("riskExplanation", "说明");
        item.put("suggestion", "建议");
        item.put("isHighRisk", isHighRisk);
        return item;
    }

    private Map<String, Object> enforceOne(Map<String, Object> item, String preference) {
        List<Map<String, Object>> result = engine.enforce(List.of(item), "合同全文", preference);
        return result.get(0);
    }

    // ==================== R1 违约金定量校验 ====================

    @Test
    @DisplayName("R1：0.5%/日违约金被 AI 标为高风险时应降为中风险")
    void penaltyCommonRateShouldNotBeHigh() {
        Map<String, Object> item = enforceOne(
                record("乙方逾期支付租金的，每逾期一日按月租金的0.5%支付违约金", "high", true),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("medium", item.get("riskLevel"));
        assertEquals(Boolean.FALSE, item.get("isHighRisk"));
        assertEquals("rule_engine", item.get("correctedBy"));
        assertNotNull(item.get("ruleReason"));
    }

    @Test
    @DisplayName("R1：2%/日违约金应修正为高风险")
    void penaltyHighRateShouldBeHigh() {
        Map<String, Object> item = enforceOne(
                record("乙方逾期支付租金的，每逾期一日按月租金的2%支付违约金", "medium", false),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("high", item.get("riskLevel"));
        assertEquals(Boolean.TRUE, item.get("isHighRisk"));
        assertEquals("rule_engine", item.get("correctedBy"));
    }

    @Test
    @DisplayName("R1：严格档下 0.6%/日即应为高风险")
    void strictPreferenceLowersHighThreshold() {
        Map<String, Object> item = enforceOne(
                record("乙方逾期支付租金的，每逾期一日按月租金的0.6%支付违约金", "medium", false),
                RiskRuleEngine.PREF_STRICT);
        assertEquals("high", item.get("riskLevel"));
    }

    @Test
    @DisplayName("R1：宽松档下 1.5%/日仍为中风险")
    void lenientPreferenceRaisesHighThreshold() {
        Map<String, Object> item = enforceOne(
                record("乙方逾期支付租金的，每逾期一日按月租金的1.5%支付违约金", "high", true),
                RiskRuleEngine.PREF_LENIENT);
        assertEquals("medium", item.get("riskLevel"));
    }

    // ==================== R2 押金条款 ====================

    @Test
    @DisplayName("R2：押金不予退还应为高风险")
    void depositNoRefundShouldBeHigh() {
        Map<String, Object> item = enforceOne(
                record("租赁期满后押金不予退还，甲方有权以房屋存在使用痕迹为由扣押押金", "medium", false),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("high", item.get("riskLevel"));
        assertEquals(Boolean.TRUE, item.get("isHighRisk"));
    }

    @Test
    @DisplayName("R2：押金三个月超过标准档阈值应为中风险")
    void depositThreeMonthsShouldBeMedium() {
        Map<String, Object> item = enforceOne(
                record("乙方应支付押金三个月作为履约保证", "high", true),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("medium", item.get("riskLevel"));
    }

    @Test
    @DisplayName("R2：验收无损坏退还押金被 AI 标高风险时应降为低风险")
    void depositNormalRefundShouldBeLow() {
        Map<String, Object> item = enforceOne(
                record("租赁期满或合同解除后，乙方结清费用并返还房屋，经甲方验收无损坏的，押金予以退还", "high", true),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("low", item.get("riskLevel"));
        assertEquals(Boolean.FALSE, item.get("isHighRisk"));
    }

    // ==================== R3 强制高风险 ====================

    @Test
    @DisplayName("R3：放弃诉讼权利条款应强制为高风险")
    void waiveLitigationShouldBeHigh() {
        Map<String, Object> item = enforceOne(
                record("双方发生任何争议，乙方均放弃诉讼权利，不得向人民法院起诉", "medium", false),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("high", item.get("riskLevel"));
        assertEquals("rule_engine", item.get("correctedBy"));
    }

    @Test
    @DisplayName("R3：单方随时涨租条款应强制为高风险")
    void arbitraryRentIncreaseShouldBeHigh() {
        Map<String, Object> item = enforceOne(
                record("甲方有权根据市场情况随时提高租金，乙方应按新标准支付", "low", false),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("high", item.get("riskLevel"));
    }

    // ==================== R4 正面条款降级 ====================

    @Test
    @DisplayName("R4：5 日内维修的保障条款被 AI 标高风险时应降为低风险")
    void positiveRepairClauseShouldBeLow() {
        Map<String, Object> item = enforceOne(
                record("房屋及附属设施因自然损耗损坏的，甲方应在收到报修通知后5日内安排维修", "high", true),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("low", item.get("riskLevel"));
    }

    // ==================== 修正透传与幂等 ====================

    @Test
    @DisplayName("与锚点一致的记录不做任何修正且不带修正标记")
    void compliantRecordIsUntouched() {
        Map<String, Object> original = record("乙方逾期支付租金的，每逾期一日按月租金的2%支付违约金", "high", true);
        Map<String, Object> item = enforceOne(original, RiskRuleEngine.PREF_STANDARD);
        assertEquals("high", item.get("riskLevel"));
        assertNull(item.get("correctedBy"));
        assertNull(item.get("ruleReason"));
        // 原始 Map 不被修改
        assertEquals("high", original.get("riskLevel"));
        assertEquals(Boolean.TRUE, original.get("isHighRisk"));
    }

    @Test
    @DisplayName("非风险条款（无关键词）不做任何修正")
    void nonMatchingRecordIsUntouched() {
        Map<String, Object> item = enforceOne(
                record("本合同一式两份，甲乙双方各执一份", "low", false),
                RiskRuleEngine.PREF_STANDARD);
        assertEquals("low", item.get("riskLevel"));
        assertNull(item.get("correctedBy"));
    }
}
