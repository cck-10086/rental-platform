package com.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("house_viewing")
public class HouseViewing {    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getHouseId() { return this.houseId; }
    public void setHouseId(Long houseId) { this.houseId = houseId; }
    public LocalDateTime getViewingTime() { return this.viewingTime; }
    public void setViewingTime(LocalDateTime viewingTime) { this.viewingTime = viewingTime; }
    public String getImpression() { return this.impression; }
    public void setImpression(String impression) { this.impression = impression; }
    public String getPros() { return this.pros; }
    public void setPros(String pros) { this.pros = pros; }
    public String getCons() { return this.cons; }
    public void setCons(String cons) { this.cons = cons; }
    public Integer getScore() { return this.score; }
    public void setScore(Integer score) { this.score = score; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @TableId
    private Long id;

    private Long userId;

    private Long houseId;

    private LocalDateTime viewingTime;

    private String impression;

    private String pros;

    private String cons;

    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
