package com.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 审查结果反馈：用户对单条审查记录标记 有用/误报。
 */
@TableName("review_feedback")
public class ReviewFeedback {    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecordId() { return this.recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFeedback() { return this.feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @TableId
    private Long id;

    /** 审查记录ID */
    private Long recordId;

    /** 用户ID */
    private Long userId;

    /** 反馈类型: useful-有用 / false_positive-误报 */
    private String feedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
