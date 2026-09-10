package com.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("house")
public class House {    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public String getAddress() { return this.address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getRentPrice() { return this.rentPrice; }
    public void setRentPrice(BigDecimal rentPrice) { this.rentPrice = rentPrice; }
    public BigDecimal getDeposit() { return this.deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
    public Double getArea() { return this.area; }
    public void setArea(Double area) { this.area = area; }
    public Integer getRooms() { return this.rooms; }
    public void setRooms(Integer rooms) { this.rooms = rooms; }
    public String getFloor() { return this.floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public String getContactName() { return this.contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return this.contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getImages() { return this.images; }
    public void setImages(String images) { this.images = images; }
    public String getNotes() { return this.notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
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

    private String address;

    private BigDecimal rentPrice;

    private BigDecimal deposit;

    private Double area;

    private Integer rooms;

    private String floor;

    private String contactName;

    private String contactPhone;

    private String images;

    private String notes;

    private String status = "viewing";

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
