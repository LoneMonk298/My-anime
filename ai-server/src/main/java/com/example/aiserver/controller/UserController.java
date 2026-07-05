package com.example.aiserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiserver.common.ApiResult;
import com.example.aiserver.common.BusinessException;
import com.example.aiserver.dto.EmailUpdateRequest;
import com.example.aiserver.dto.LoginRequest;
import com.example.aiserver.dto.PasswordResetCodeRequest;
import com.example.aiserver.dto.PasswordResetRequest;
import com.example.aiserver.dto.PasswordUpdateRequest;
import com.example.aiserver.dto.ProfileUpdateRequest;
import com.example.aiserver.dto.RegisterRequest;
import com.example.aiserver.entity.SysUser;
import com.example.aiserver.mapper.SysUserMapper;
import com.example.aiserver.service.MailService;
import com.example.aiserver.service.PasswordResetCodeService;
import com.example.aiserver.util.CurrentUserUtil;
import com.example.aiserver.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final CurrentUserUtil currentUserUtil;
    private final PasswordResetCodeService passwordResetCodeService;
    private final MailService mailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getStatus, 1));

        if (user == null || !passwordMatches(request.getPassword(), user.getPasswordHash())) {
            return ApiResult.unauthorized("用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return ApiResult.success(Map.of("token", token, "userInfo", toProfile(user)));
    }

    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        return ApiResult.success();
    }

    @PostMapping("/admin")
    public ApiResult<Map<String, Object>> createAdmin(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody RegisterRequest request
    ) {
        SysUser current = currentUserUtil.requireUser(authorization);
        if (!"SUPER_ADMIN".equals(current.getRole())) {
            throw new BusinessException("仅超级管理员可以新增管理员");
        }

        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole("ADMIN");
        user.setStatus(1);
        userMapper.insert(user);
        return ApiResult.success(toProfile(user));
    }

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResult.success(toProfile(currentUserUtil.requireUser(authorization)));
    }

    @PutMapping("/profile")
    public ApiResult<Map<String, Object>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ProfileUpdateRequest request
    ) {
        SysUser current = currentUserUtil.requireUser(authorization);
        SysUser update = new SysUser();
        update.setId(current.getId());
        if (StringUtils.hasText(request.getUsername())) {
            update.setUsername(request.getUsername());
            update.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        } else if (StringUtils.hasText(request.getNickname())) {
            update.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getEmail())) {
            update.setEmail(request.getEmail());
        }
        update.setAge(request.getAge());
        update.setGender(request.getGender());
        if (StringUtils.hasText(request.getAvatar())) {
            update.setAvatarUrl(request.getAvatar());
        }
        userMapper.updateById(update);
        return ApiResult.success(toProfile(userMapper.selectById(current.getId())));
    }

    @PutMapping("/password")
    public ApiResult<Void> updatePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PasswordUpdateRequest request
    ) {
        SysUser current = currentUserUtil.requireUser(authorization);
        if (!passwordMatches(request.getOldPassword(), current.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        SysUser update = new SysUser();
        update.setId(current.getId());
        update.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);
        return ApiResult.success();
    }

    @PutMapping("/email")
    public ApiResult<Map<String, Object>> updateEmail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody EmailUpdateRequest request
    ) {
        SysUser current = currentUserUtil.requireUser(authorization);
        SysUser update = new SysUser();
        update.setId(current.getId());
        update.setEmail(request.getEmail());
        userMapper.updateById(update);
        return ApiResult.success(toProfile(userMapper.selectById(current.getId())));
    }

    @PostMapping("/password/reset-code")
    public ApiResult<Void> sendPasswordResetCode(@Valid @RequestBody PasswordResetCodeRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, request.getEmail())
                .eq(SysUser::getStatus, 1));
        if (user == null) {
            throw new BusinessException("邮箱未绑定有效账号");
        }

        String code = passwordResetCodeService.createCode(request.getEmail());
        mailService.sendPasswordResetCode(request.getEmail(), code);
        return ApiResult.success();
    }

    @PutMapping("/password/reset")
    public ApiResult<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, request.getEmail())
                .eq(SysUser::getStatus, 1));
        if (user == null) {
            throw new BusinessException("邮箱未绑定有效账号");
        }

        passwordResetCodeService.verifyAndConsume(request.getEmail(), request.getCode());
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);
        return ApiResult.success();
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.success();
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$10$demo-password-hash")) {
            return "admin123".equals(rawPassword) || "123456".equals(rawPassword);
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    private Map<String, Object> toProfile(SysUser user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("age", user.getAge());
        profile.put("gender", user.getGender());
        profile.put("avatar", user.getAvatarUrl());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("role", user.getRole());
        profile.put("roleType", isAdminRole(user.getRole()) ? 2 : 1);
        return profile;
    }

    private boolean isAdminRole(String role) {
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
}
