package com.rental.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 安全上下文工具单元测试。
 */
class SecurityUtilTest {

    @Test
    void getCurrentUserIdThrowsWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThrows(RuntimeException.class, SecurityUtil::getCurrentUserId);
    }
}
