package com.rental.service;

import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 法律知识库（RAG）单元测试：语料加载、条文分块、语义检索。
 */
class LegalKnowledgeServiceTest {

    private final LegalKnowledgeService service = new LegalKnowledgeService();

    @Test
    void searchReturnsFiveRelevantArticles() {
        service.init();
        List<TextSegment> hits = service.searchTop5("租房押金和租金应该怎么约定");
        assertEquals(5, hits.size(), "应返回 Top-5 条文");
        assertTrue(hits.stream().anyMatch(s -> s.text().contains("押金")),
                "检索押金问题应命中包含押金的条文");
        // 每个条文块都应带有来源法名元数据
        assertTrue(hits.stream().allMatch(s -> s.metadata().getString("source") != null));
    }

    @Test
    void searchRepairDutyFindsArticle712() {
        service.init();
        List<TextSegment> hits = service.searchTop5("出租人应当履行租赁物的维修义务吗");
        assertTrue(hits.size() >= 5);
        // 《民法典》第七百一十二条与查询语句几乎一致，应进入前两名
        boolean found = hits.stream().limit(2).anyMatch(s ->
                "第七百一十二条".equals(s.metadata().getString("article"))
                        && s.text().contains("维修义务"));
        assertTrue(found, "维修义务检索应命中民法典第七百一十二条");
    }
}
