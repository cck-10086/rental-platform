package com.rental.service;

import java.util.Map;
import java.util.List;

public interface DeepSeekService {
    Map<String, String> chat(String question, String conversationId, Long userId);
    Map<String, Object> reviewContract(String contractContent);
    String extractTextFromFile(byte[] fileBytes, String fileName);
    List<Map<String, Object>> listConversations(Long userId);
    List<Map<String, Object>> getConversationMessages(String conversationId, Long userId);
    void deleteConversation(String conversationId, Long userId);
}
