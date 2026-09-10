package com.rental.service;

import com.rental.common.PageResult;
import com.rental.entity.User;

import java.util.Map;

/**
 * 管理后台服务：仅管理员可调用。
 */
public interface AdminService {
    PageResult<User> listUsers(String keyword, long current, long size);

    void updateUserStatus(Long targetId, Integer status);

    void updateUserRole(Long targetId, String role);

    void resetPassword(Long targetId, String password);

    PageResult<Map<String, Object>> listContracts(String keyword, long current, long size);

    PageResult<Map<String, Object>> listHouses(String keyword, long current, long size);

    PageResult<Map<String, Object>> listExpenses(String keyword, long current, long size);

    PageResult<Map<String, Object>> listRepairs(String keyword, long current, long size);

    PageResult<Map<String, Object>> listMoveOuts(String keyword, long current, long size);

    Map<String, Object> stats();
}
