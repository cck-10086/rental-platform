package com.rental.service.impl;

import com.rental.common.JwtUtil;
import com.rental.dto.LoginDTO;
import com.rental.dto.LoginResultDTO;
import com.rental.dto.RegisterDTO;
import com.rental.dto.UpdateProfileDTO;
import com.rental.entity.User;
import com.rental.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试：注册、登录、资料更新白名单。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded");
        user.setNickname("管理员");
        user.setRole("admin");
        user.setStatus(1);
        return user;
    }

    @Test
    void registerDuplicateUsernameThrows() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");
        RuntimeException e = assertThrows(RuntimeException.class, () -> userService.register(dto));
        assertEquals("用户名已存在", e.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginSuccessReturnsTokenWithRole() {
        when(userMapper.selectOne(any())).thenReturn(buildUser());
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "admin", "admin")).thenReturn("token-abc");

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");
        LoginResultDTO result = userService.login(dto);

        assertEquals("token-abc", result.getToken());
        assertEquals("admin", result.getUsername());
        assertEquals("admin", result.getRole());
    }

    @Test
    void loginWrongPasswordThrows() {
        when(userMapper.selectOne(any())).thenReturn(buildUser());
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("wrong");
        assertThrows(RuntimeException.class, () -> userService.login(dto));
    }

    @Test
    void loginDisabledAccountThrows() {
        User user = buildUser();
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");
        RuntimeException e = assertThrows(RuntimeException.class, () -> userService.login(dto));
        assertEquals("账号已被禁用", e.getMessage());
    }

    @Test
    void updateProfileOnlyChangesNicknameAndPhone() {
        User existing = buildUser();
        when(userMapper.selectById(1L)).thenReturn(existing);

        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("新昵称");
        dto.setPhone("13800000000");

        // 模拟已登录用户（principal 为用户ID）
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        try {
            userService.updateProfile(dto);
        } finally {
            SecurityContextHolder.clearContext();
        }

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User updated = captor.getValue();
        assertEquals("新昵称", updated.getNickname());
        assertEquals("13800000000", updated.getPhone());
        // 角色与状态不允许被修改
        assertEquals("admin", updated.getRole());
        assertEquals(1, updated.getStatus());
    }
}
