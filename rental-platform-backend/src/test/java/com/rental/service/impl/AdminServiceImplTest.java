package com.rental.service.impl;

import com.rental.entity.User;
import com.rental.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 管理后台服务单元测试：角色校验、自我保护、密码重置。
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole("user");
        user.setStatus(1);
        return user;
    }

    @Test
    void updateUserRoleRejectsInvalidRole() {
        loginAs(1L);
        try {
            RuntimeException e = assertThrows(RuntimeException.class, () -> adminService.updateUserRole(2L, "super"));
            assertEquals("角色只能是 admin 或 user", e.getMessage());
            // 角色校验先于查库执行，不应访问数据库
            verify(userMapper, never()).selectById(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void updateUserRoleCannotOperateSelf() {
        loginAs(1L);
        try {
            RuntimeException e = assertThrows(RuntimeException.class, () -> adminService.updateUserRole(1L, "user"));
            assertEquals("不能修改自己的角色", e.getMessage());
            verify(userMapper, never()).updateById(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void resetPasswordEncodesNewPassword() {
        loginAs(1L);
        try {
            when(userMapper.selectById(2L)).thenReturn(buildUser(2L));
            when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");

            adminService.resetPassword(2L, "newpass");

            verify(userMapper).updateById(argThat(u -> "encoded-new".equals(u.getPassword())));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void loginAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
