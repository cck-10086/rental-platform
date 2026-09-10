package com.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("repair_request")
public class RepairRequest {    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getContractId() { return this.contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    public String getImages() { return this.images; }
    public void setImages(String images) { this.images = images; }
    public String getUrgency() { return this.urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public String getHandlerNote() { return this.handlerNote; }
    public void setHandlerNote(String handlerNote) { this.handlerNote = handlerNote; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getDeleted() { return this.deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    @TableId
    private Long id;

    private Long userId;

    private Long contractId;

    private String title;

    private String description;

    private String images;

    private String urgency;

    private String status = "pending";

    private String handlerNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
