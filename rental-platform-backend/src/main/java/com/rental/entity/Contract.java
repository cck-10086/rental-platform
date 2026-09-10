package com.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("contract")
public class Contract {    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public String getFileName() { return this.fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return this.filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileType() { return this.fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getContent() { return this.content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public String getRiskLevel() { return this.riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Integer getRiskCount() { return this.riskCount; }
    public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return this.deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    @TableId
    private Long id;

    private Long userId;

    private String title;

    private String fileName;

    private String filePath;

    private String fileType;

    private String content;

    private String status = "pending";

    private String riskLevel;

    private Integer riskCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
