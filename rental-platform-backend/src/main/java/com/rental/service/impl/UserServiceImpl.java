package com.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.common.JwtUtil;
import com.rental.common.SecurityUtil;
import com.rental.dto.LoginDTO;
import com.rental.dto.LoginResultDTO;
import com.rental.dto.RegisterDTO;
import com.rental.dto.UpdateProfileDTO;
import com.rental.entity.User;
import com.rental.mapper.UserMapper;
import com.rental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);


    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void register(RegisterDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        userMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());
    }

    @Override
    public LoginResultDTO login(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginResultDTO result = new LoginResultDTO();
        result.setToken(token);
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setNickname(user.getNickname());
        result.setRole(user.getRole());
        log.info("用户登录成功: {}", user.getUsername());
        return result;
    }

    @Override
    public User getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        User existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        // 仅允许更新昵称和手机号，其他字段（角色/状态/密码等）一律不允许客户端修改
        if (dto.getNickname() != null) {
            existing.setNickname(dto.getNickname());
        }
        if (dto.getPhone() != null) {
            existing.setPhone(dto.getPhone());
        }
        userMapper.updateById(existing);
        log.info("用户信息更新成功: {}", userId);
    }
}
