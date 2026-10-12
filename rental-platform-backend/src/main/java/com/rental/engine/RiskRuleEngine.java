package com.rental.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 风险定级规则引擎：在 AI 审查输出存库前，对定量类条款做确定性二次校验，
 * 修正与锚点口径不一致的定级，并将修正过程标记到记录上。
 *
 * 设计说明：
 * - 纯逻辑组件，不依赖数据库，便于单元测试；
 * - 档位阈值与审查提示词锚点保持同一口径，偏好仅调整数值边界；
 * - 只修正定级与 isHighRisk，不改动 AI 的风险说明与建议文本。
 */
@Component
public class RiskRuleEngine {

    /** 偏好档位：宽松 */
    public static final String PREF_LENIENT = "lenient";
    /** 偏好档位：标准 */
    public static final String PREF_STANDARD = "standard";
    /** 偏好档位：严格 */
    public static final String PREF_STRICT = "strict";

    /** 等级权重，用于比较高低 */
    private static int levelWeight(String level) {
        if (level == null) return 0;
        return switch (level.toLowerCase()) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    /** 日违约金百分比提取：如 "0.5%"、"百分之十"仅支持数字写法 */
    private static final Pattern DAILY_RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

    /** 押金月数提取：如 "押金为两个月"、"押金3个月" */
    private static final Pattern DEPOSIT_MONTHS_PATTERN =
            Pattern.compile("押金[^。；;]*?(一|两|三|四|五|六|七|八|九|十|\\d+)\\s*个?月");

    /** 强制高风险关键词（放弃诉权、人身伤亡免责、单方随时涨租/解除/入户） */
    private static final List<Pattern> FORCE_HIGH_PATTERNS = List.of(
            Pattern.compile("放弃.{0,6}(诉讼|诉权|仲裁)|不得.{0,4}起诉"),
            Pattern.compile("(人身|伤亡|伤害|损失).{0,10}概不负责"),
            Pattern.compile("随时.{0,6}(提高|上调|调整|涨).{0,4}租金|单方.{0,6}调整租金"),
            Pattern.compile("随时.{0,6}(解除|终止|收回)|有权随时.{0,8}(进入|入户)")
    );

    /** 保障性正面条款特征（触发降级） */
    private static final List<Pattern> POSITIVE_PATTERNS = List.of(
            Pattern.compile("(5|五)\\s*(日|天|个工作日)内.{0,10}维修"),
            Pattern.compile("提前(30|三十)日.{0,8}书面通知"),
            Pattern.compile("协商一致|双方协商"),
            Pattern.compile("(验收)?无(损坏|损毁).{0,20}退还")
    );

    /** 押金不予退还类关键词 */
    private static final Pattern DEPOSIT_NO_REFUND_PATTERN =
            Pattern.compile("(不予退还|概不退还|不予退回|不退|扣押|没收|以.{0,8}(使用)?痕迹.{0,8}(扣|抵))");

    /**
     * 对 AI 审查输出执行规则校验，返回修正后的记录列表（原列表不被修改）。
     *
     * @param records          AI 输出的风险记录（元素含 riskLevel/clauseContent/isHighRisk 等键）
     * @param contractContent  合同全文（备用上下文）
     * @param preference      风险偏好档位：lenient/standard/strict
     */
    public List<Map<String, Object>> enforce(List<Map<String, Object>> records,
                                             String contractContent,
                                             String preference) {
        List<Map<String, Object>> result = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            result.add(enforceOne(record, contractContent, preference));
        }
        return result;
    }

    /** 对单条记录依序应用 R3 强制高 → R2 押金 → R1 违约金 → R4 正面降级 */
    private Map<String, Object> enforceOne(Map<String, Object> record, String contractContent, String preference) {
        // 复制一份，避免修改调用方原始 Map
        Map<String, Object> item = new java.util.LinkedHashMap<>(record);
        String clause = str(item.get("clauseContent"));
        String level = str(item.get("riskLevel")).toLowerCase();
        String expected = level;

        String reason = null;
        // R3：强制高风险关键词
        String forceHigh = matchForceHigh(clause);
        if (forceHigh != null) {
            expected = "high";
            reason = "命中强制高风险规则：" + forceHigh;
        }
        // R2：押金条款（未命中强制高时）
        if (reason == null && clause.contains("押金")) {
            DepositVerdict depositVerdict = judgeDeposit(clause, preference);
            if (depositVerdict != null) {
                expected = depositVerdict.level();
                reason = depositVerdict.reason();
            }
        }
        // R1：违约金日利率定量校验
        if (reason == null && isPenaltyClause(clause)) {
            BigDecimal rate = extractDailyRate(clause);
            if (rate != null) {
                PenaltyVerdict verdict = judgePenalty(rate, preference);
                // 违约金是数值规则，与 AI 定级不一致时始终修正（含向上修正）
                if (!verdict.level().equals(level)) {
                    expected = verdict.level();
                    reason = verdict.reason();
                }
            }
            if (reason == null && clause.contains("三倍")) {
                expected = "high";
                reason = "违约金/赔偿达三倍，金额达月租金量级，应为高风险";
            }
        }
        // R4：保障性正面条款降级（仅当此前无规则命中且 AI 标为高）
        if (reason == null && "high".equals(level) && matchPositive(clause) != null) {
            expected = "low";
            reason = "该条款为常见保障性约定，不应定为高风险";
        }

        // 应用修正
        if (reason != null && !expected.equals(level)) {
            item.put("riskLevel", expected);
            item.put("isHighRisk", "high".equals(expected));
            item.put("correctedBy", "rule_engine");
            item.put("ruleReason", reason);
        }
        return item;
    }

    // ==================== R1 违约金定量校验 ====================

    private record PenaltyVerdict(String level, String reason) {}

    /** 判断是否为违约金条款 */
    private boolean isPenaltyClause(String clause) {
        return clause != null && clause.contains("违约");
    }

    /** 提取条款中的百分比数值（取最大值，条款中通常只有一个） */
    private BigDecimal extractDailyRate(String clause) {
        if (clause == null) return null;
        Matcher matcher = DAILY_RATE_PATTERN.matcher(clause);
        BigDecimal max = null;
        while (matcher.find()) {
            BigDecimal value = new BigDecimal(matcher.group(1));
            if (max == null || value.compareTo(max) > 0) {
                max = value;
            }
        }
        return max;
    }

    /** 按档位阈值判定违约金日利率的期望等级 */
    private PenaltyVerdict judgePenalty(BigDecimal ratePercent, String preference) {
        // 各档位：{不高于该值视为常见约定的上限, medium 上界, high 下界}
        double commonMax;
        double highMin;
        switch (prefKey(preference)) {
            case PREF_LENIENT -> { commonMax = 1.0; highMin = 2.0; }
            case PREF_STRICT -> { commonMax = 0.3; highMin = 0.5; }
            default -> { commonMax = 0.5; highMin = 1.0; }
        }
        double rate = ratePercent.doubleValue();

        if (rate > highMin) {
            return new PenaltyVerdict("high", "违约金日利率 " + rate + "% 超过 " + highMin + "%，单月累计明显超过月租金，应为高风险");
        }
        if (rate > commonMax) {
            return new PenaltyVerdict("medium", "违约金日利率 " + rate + "% 属可依法请求调减的范围，应为中风险");
        }
        // 不超过常见上限：AI 若标为 high 应降级（0.5%/日属常见约定）
        return new PenaltyVerdict("medium", "违约金日利率 " + rate + "% 属常见约定，最高只应定为中风险");
    }

    // ==================== R2 押金条款校验 ====================

    private record DepositVerdict(String level, String reason) {}

    private DepositVerdict judgeDeposit(String clause, String preference) {
        // 押金不予退还/扣押类 → 强制高风险（各档位一致）
        if (DEPOSIT_NO_REFUND_PATTERN.matcher(clause).find()) {
            return new DepositVerdict("high", "押金存在不予退还或以使用痕迹扣押的约定，应为高风险");
        }
        // 正常退还约定 → 低风险
        if (clause.contains("退还") && clause.matches(".*无(损坏|损毁).*")) {
            return new DepositVerdict("low", "押金按验收无损坏退还属常见约定，应为低风险");
        }
        // 月数校验：超过档位阈值视为中风险
        Integer months = extractDepositMonths(clause);
        if (months != null) {
            int threshold = switch (prefKey(preference)) {
                case PREF_LENIENT -> 3;
                case PREF_STRICT -> 1;
                default -> 2;
            };
            if (months > threshold) {
                return new DepositVerdict("medium",
                        "押金达 " + months + " 个月，超过" + prefName(preference) + "档阈值 " + threshold + " 个月，应为中风险");
            }
        }
        return null;
    }

    /** 从押金条款中提取月数，中文数字与阿拉伯数字均支持 */
    private Integer extractDepositMonths(String clause) {
        Matcher matcher = DEPOSIT_MONTHS_PATTERN.matcher(clause);
        if (!matcher.find()) {
            return null;
        }
        String token = matcher.group(1);
        return switch (token) {
            case "一" -> 1;
            case "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> {
                try {
                    yield Integer.parseInt(token);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    // ==================== R3 / R4 关键词匹配 ====================

    /** 返回命中的强制高风险规则描述，未命中返回 null */
    private String matchForceHigh(String clause) {
        if (clause == null) return null;
        for (Pattern pattern : FORCE_HIGH_PATTERNS) {
            if (pattern.matcher(clause).find()) {
                return pattern.pattern();
            }
        }
        return null;
    }

    /** 返回命中的保障性正面条款特征描述，未命中返回 null */
    private String matchPositive(String clause) {
        if (clause == null) return null;
        for (Pattern pattern : POSITIVE_PATTERNS) {
            if (pattern.matcher(clause).find()) {
                return pattern.pattern();
            }
        }
        return null;
    }

    // ==================== 工具方法 ====================

    private String prefKey(String preference) {
        if (PREF_LENIENT.equals(preference) || PREF_STRICT.equals(preference)) {
            return preference;
        }
        return PREF_STANDARD;
    }

    private String prefName(String preference) {
        return switch (prefKey(preference)) {
            case PREF_LENIENT -> "宽松";
            case PREF_STRICT -> "严格";
            default -> "标准";
        };
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
