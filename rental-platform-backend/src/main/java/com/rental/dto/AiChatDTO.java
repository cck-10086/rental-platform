package com.rental.dto;

import jakarta.validation.constraints.NotBlank;

public class AiChatDTO {    public String getQuestion() { return this.question; }
    public void setQuestion(String question) { this.question = question; }
    public String getConversationId() { return this.conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    @NotBlank(message = "问题不能为空")
    private String question;

    private String conversationId;
}
