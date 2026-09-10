package com.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("review_record")
public class ReviewRecord {    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getContractId() { return this.contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getClauseContent() { return this.clauseContent; }
    public void setClauseContent(String clauseContent) { this.clauseContent = clauseContent; }
    public String getRiskType() { return this.riskType; }
    public void setRiskType(String riskType) { this.riskType = riskType; }
    public String getRiskLevel() { return this.riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskExplanation() { return this.riskExplanation; }
    public void setRiskExplanation(String riskExplanation) { this.riskExplanation = riskExplanation; }
    public String getSuggestion() { return this.suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Integer getIsHighRisk() { return this.isHighRisk; }
    public void setIsHighRisk(Integer isHighRisk) { this.isHighRisk = isHighRisk; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @TableId
    private Long id;

    private Long contractId;

    private Long userId;

    private String clauseContent;

    private String riskType;

    private String riskLevel;

    private String riskExplanation;

    private String suggestion;

    private Integer isHighRisk = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
