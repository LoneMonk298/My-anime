package com.example.aiserver.util;

import com.example.aiserver.common.BusinessException;
import com.example.aiserver.entity.SysUser;
import com.example.aiserver.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserUtil {
    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;

    public Long requireUserId(String authorization) {
        Long userId = jwtUtil.parseUserId(authorization);
        if (userId == null) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }
        return userId;
    }

    public SysUser requireUser(String authorization) {
        SysUser user = userMapper.selectById(requireUserId(authorization));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("用户不存在或已被禁用");
        }
        return user;
    }
}
