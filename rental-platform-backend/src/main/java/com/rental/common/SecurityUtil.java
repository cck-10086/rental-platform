package com.rental.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：统一从 JWT 过滤器写入的认证信息中获取当前用户ID。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 获取当前登录用户ID。
     *
     * @throws RuntimeException 未登录或认证信息异常时抛出
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new RuntimeException("未登录或登录已过期");
        }
        return userId;
    }
}
