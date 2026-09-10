package com.rental.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理员修改用户角色请求体。
 */
public class UpdateUserRoleDTO {
    @NotBlank(message = "角色不能为空")
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
