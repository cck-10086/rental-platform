package com.rental.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具单元测试：生成、解析、过期、篡改。
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        // 通过反射注入配置，避免依赖 Spring 容器
        setField(jwtUtil, "secret", "rental-test-secret-key-0123456789-0123456789");
        setField(jwtUtil, "expiration", 3600000L);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void generateTokenAndParse() {
        String token = jwtUtil.generateToken(1L, "admin", "admin");
        assertNotNull(token);
        assertEquals("1", jwtUtil.parseToken(token).getSubject());
        assertEquals("admin", jwtUtil.parseToken(token).get("username", String.class));
        assertEquals("admin", jwtUtil.parseToken(token).get("role", String.class));
        assertEquals(1L, jwtUtil.getUserId(token));
    }

    @Test
    void expiredTokenRejected() throws Exception {
        setField(jwtUtil, "expiration", -1000L);
        String token = jwtUtil.generateToken(1L, "admin", "admin");
        assertThrows(Exception.class, () -> jwtUtil.parseToken(token));
    }

    @Test
    void tamperedTokenRejected() {
        String token = jwtUtil.generateToken(1L, "admin", "admin");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(Exception.class, () -> jwtUtil.parseToken(tampered));
    }
}
