package com.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 风险偏好全局配置（单行表）：lenient-宽松 / standard-标准 / strict-严格
 */
@TableName("risk_config")
public class RiskConfig {
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getPreference() { return this.preference; }
    public void setPreference(String preference) { this.preference = preference; }
    public LocalDateTime getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @TableId
    private Long id;

    private String preference;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
