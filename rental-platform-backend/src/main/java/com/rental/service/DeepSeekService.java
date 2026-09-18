package com.rental.service;

import java.util.Map;
import java.util.List;

public interface DeepSeekService {
    Map<String, String> chat(String question, String conversationId, Long userId);

    /**
     * 流式对话：DeepSeek 请求 stream=true，逐段回调 onDelta，完成后落库（复用会话历史逻辑）。
     */
    void chatStream(String question, String conversationId, Long userId, StreamListener listener);

    /**
     * 流式对话监听器：onStart 在确定会话 ID 后回调，onDelta 逐段回调增量内容。
     */
    interface StreamListener {
        default void onStart(String conversationId) {
        }

        void onDelta(String delta);
    }

    Map<String, Object> reviewContract(String contractContent);
    String extractTextFromFile(byte[] fileBytes, String fileName);
    List<Map<String, Object>> listConversations(Long userId);
    List<Map<String, Object>> getConversationMessages(String conversationId, Long userId);
    void deleteConversation(String conversationId, Long userId);
}
