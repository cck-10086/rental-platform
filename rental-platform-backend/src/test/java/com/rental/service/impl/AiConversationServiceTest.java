package com.rental.service.impl;

import com.rental.entity.AiConversation;
import com.rental.mapper.AiConversationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 会话管理单元测试：列表聚合、消息组装、删除。
 */
@ExtendWith(MockitoExtension.class)
class AiConversationServiceTest {

    @Mock
    private AiConversationMapper aiConversationMapper;

    @InjectMocks
    private DeepSeekServiceImpl service;

    private AiConversation record(Long id, String conversationId, String question, String answer) {
        AiConversation record = new AiConversation();
        record.setId(id);
        record.setUserId(1L);
        record.setConversationId(conversationId);
        record.setQuestion(question);
        record.setAnswer(answer);
        // id=2 的 c1 追问时间最晚（10:07），c2 为 10:03，确保 c1 排在列表首位
        record.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, id == 2L ? 7 : id.intValue()));
        return record;
    }

    @Test
    void listConversationsGroupsByConversationId() {
        when(aiConversationMapper.selectList(any())).thenReturn(List.of(
                record(1L, "c1", "第一个问题", "回答1"),
                record(2L, "c1", "追问", "回答2"),
                record(3L, "c2", "另一个会话", "回答3")
        ));
        List<Map<String, Object>> list = service.listConversations(1L);
        assertEquals(2, list.size());
        // c1 最后活动时间更晚，应排在前面
        Map<String, Object> first = list.get(0);
        assertEquals("c1", first.get("conversationId"));
        assertEquals(2, first.get("messageCount"));
        assertEquals("第一个问题", first.get("title"));
        assertEquals("c2", list.get(1).get("conversationId"));
    }

    @Test
    void listConversationsTruncatesLongTitle() {
        String longQuestion = "这是一个非常非常非常非常非常非常非常非常非常非常非常长的问题标题测试";
        when(aiConversationMapper.selectList(any())).thenReturn(List.of(
                record(1L, "c1", longQuestion, "回答")
        ));
        List<Map<String, Object>> list = service.listConversations(1L);
        assertEquals(30, ((String) list.get(0).get("title")).length());
    }

    @Test
    void getConversationMessagesBuildsUserAssistantPairs() {
        when(aiConversationMapper.selectList(any())).thenReturn(List.of(
                record(1L, "c1", "问题一", "回答一"),
                record(2L, "c1", "问题二", "回答二")
        ));
        List<Map<String, Object>> messages = service.getConversationMessages("c1", 1L);
        assertEquals(4, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("问题一", messages.get(0).get("content"));
        assertEquals("assistant", messages.get(1).get("role"));
        assertEquals("回答一", messages.get(1).get("content"));
    }

    @Test
    void deleteConversationDeletesOwnRecords() {
        service.deleteConversation("c1", 1L);
        verify(aiConversationMapper).delete(any());
    }
}
