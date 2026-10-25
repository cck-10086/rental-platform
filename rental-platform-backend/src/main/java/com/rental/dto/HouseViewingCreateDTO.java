package com.rental.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class HouseViewingCreateDTO {

    @NotNull(message = "房源不能为空")
    @Positive(message = "房源ID必须为正数")
    private Long houseId;

    @NotNull(message = "看房时间不能为空")
    private LocalDateTime viewingTime;

    @NotBlank(message = "总体印象不能为空")
    @Size(max = 1000, message = "总体印象不能超过1000个字符")
    private String impression;

    @Size(max = 1000, message = "优点不能超过1000个字符")
    private String pros;

    @Size(max = 1000, message = "不足不能超过1000个字符")
    private String cons;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于1分")
    @Max(value = 5, message = "评分不能高于5分")
    private Integer score;

    public Long getHouseId() {
        return houseId;
    }

    public void setHouseId(Long houseId) {
        this.houseId = houseId;
    }

    public LocalDateTime getViewingTime() {
        return viewingTime;
    }

    public void setViewingTime(LocalDateTime viewingTime) {
        this.viewingTime = viewingTime;
    }

    public String getImpression() {
        return impression;
    }

    public void setImpression(String impression) {
        this.impression = impression;
    }

    public String getPros() {
        return pros;
    }

    public void setPros(String pros) {
        this.pros = pros;
    }

    public String getCons() {
        return cons;
    }

    public void setCons(String cons) {
        this.cons = cons;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
