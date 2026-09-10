package com.rental.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员修改用户状态（启用/禁用）请求体。
 */
public class UpdateUserStatusDTO {
    @NotNull(message = "状态不能为空")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
