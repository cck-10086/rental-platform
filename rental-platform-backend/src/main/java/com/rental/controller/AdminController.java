package com.rental.controller;

import com.rental.common.PageResult;
import com.rental.common.Result;
import com.rental.dto.ResetPasswordDTO;
import com.rental.dto.UpdateUserRoleDTO;
import com.rental.dto.UpdateUserStatusDTO;
import com.rental.entity.User;
import com.rental.service.AdminService;
import com.rental.service.RiskConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台接口：仅 admin 角色可访问。
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    private final RiskConfigService riskConfigService;

    public AdminController(AdminService adminService, RiskConfigService riskConfigService) {
        this.adminService = adminService;
        this.riskConfigService = riskConfigService;
    }

    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listUsers(keyword, current, size));
    }

    @PutMapping("/users/{id}/status")
    public Result<?> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusDTO dto) {
        adminService.updateUserStatus(id, dto.getStatus());
        return Result.success();
    }

    @PutMapping("/users/{id}/role")
    public Result<?> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleDTO dto) {
        adminService.updateUserRole(id, dto.getRole());
        return Result.success();
    }

    @PutMapping("/users/{id}/password")
    public Result<?> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        adminService.resetPassword(id, dto.getPassword());
        return Result.success();
    }

    @GetMapping("/contracts")
    public Result<PageResult<Map<String, Object>>> listContracts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listContracts(keyword, current, size));
    }

    @GetMapping("/houses")
    public Result<PageResult<Map<String, Object>>> listHouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listHouses(keyword, current, size));
    }

    @GetMapping("/expenses")
    public Result<PageResult<Map<String, Object>>> listExpenses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listExpenses(keyword, current, size));
    }

    @GetMapping("/repairs")
    public Result<PageResult<Map<String, Object>>> listRepairs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listRepairs(keyword, current, size));
    }

    @GetMapping("/move-outs")
    public Result<PageResult<Map<String, Object>>> listMoveOuts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listMoveOuts(keyword, current, size));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(adminService.stats());
    }

    /** 获取当前风险偏好档位：lenient-宽松 / standard-标准 / strict-严格 */
    @GetMapping("/risk-config")
    public Result<Map<String, Object>> getRiskConfig() {
        return Result.success(Map.of("preference", riskConfigService.getPreference()));
    }

    /** 更新风险偏好档位，切换后对新审查立即生效 */
    @PutMapping("/risk-config")
    public Result<?> updateRiskConfig(@RequestBody Map<String, String> body) {
        riskConfigService.updatePreference(body.get("preference"));
        return Result.success();
    }
}
