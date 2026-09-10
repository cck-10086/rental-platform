package com.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("ai_conversation")
public class AiConversation {    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getQuestion() { return this.question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return this.answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getConversationId() { return this.conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @TableId
    private Long id;

    private Long userId;

    private String question;

    private String answer;

    private String conversationId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
