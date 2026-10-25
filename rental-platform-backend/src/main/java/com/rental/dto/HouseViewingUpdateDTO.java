package com.rental.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class HouseViewingUpdateDTO extends HouseViewingCreateDTO {

    @NotNull(message = "看房记录ID不能为空")
    @Positive(message = "看房记录ID必须为正数")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
