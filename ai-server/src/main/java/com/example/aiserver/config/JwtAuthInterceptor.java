package com.example.aiserver.config;

import com.example.aiserver.common.ApiResult;
import com.example.aiserver.entity.SysUser;
import com.example.aiserver.mapper.SysUserMapper;
import com.example.aiserver.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {
    private static final String CURRENT_USER_ATTR = "CURRENT_USER";
    private static final String CURRENT_USER_ID_ATTR = "CURRENT_USER_ID";

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> publicPaths = List.of(
            "/user/login",
            "/user/register",
            "/user/password/reset-code",
            "/user/password/reset",
            "/uploads/**"
    );

    private final List<String> adminPaths = List.of(
            "/user/admin"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = requestPath(request);
        if (isPublicRequest(request, path)) {
            return true;
        }

        SysUser user = authenticate(request, response);
        if (user == null) {
            return false;
        }

        request.setAttribute(CURRENT_USER_ATTR, user);
        request.setAttribute(CURRENT_USER_ID_ATTR, user.getId());

        if (requiresAdmin(request, path) && !isAdmin(user)) {
            writeJson(response, HttpStatus.FORBIDDEN.value(), ApiResult.unauthorized("无权限访问"));
            return false;
        }

        return true;
    }

    private SysUser authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            writeJson(response, HttpStatus.UNAUTHORIZED.value(), ApiResult.unauthorized("未登录或登录已过期"));
            return null;
        }

        try {
            Long userId = jwtUtil.parseUserId(authorization);
            if (userId == null) {
                writeJson(response, HttpStatus.UNAUTHORIZED.value(), ApiResult.unauthorized("未登录或登录已过期"));
                return null;
            }

            SysUser user = userMapper.selectById(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                writeJson(response, HttpStatus.UNAUTHORIZED.value(), ApiResult.unauthorized("用户不存在或已被禁用"));
                return null;
            }
            return user;
        } catch (JwtException | IllegalArgumentException ex) {
            writeJson(response, HttpStatus.UNAUTHORIZED.value(), ApiResult.unauthorized("Token 无效或已过期"));
            return null;
        }
    }

    private boolean isPublicRequest(HttpServletRequest request, String path) {
        if ("GET".equalsIgnoreCase(request.getMethod()) && isPublicArticleRead(request, path)) {
            return true;
        }
        return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isPublicArticleRead(HttpServletRequest request, String path) {
        if ("/article/category/tree".equals(path)) {
            return !"true".equalsIgnoreCase(request.getParameter("includeDisabled"));
        }
        return ("/article/page".equals(path) && "1".equals(request.getParameter("status")))
                || pathMatcher.match("/article/{id}/view", path);
    }

    private boolean requiresAdmin(HttpServletRequest request, String path) {
        if (adminPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return true;
        }
        if ("/article/category/tree".equals(path)) {
            return "true".equalsIgnoreCase(request.getParameter("includeDisabled"));
        }
        if ("/article/page".equals(path)) {
            return !"1".equals(request.getParameter("status"));
        }
        if (pathMatcher.match("/article/{id}", path)) {
            return true;
        }
        if (pathMatcher.match("/article/**", path)) {
            return !"GET".equalsIgnoreCase(request.getMethod());
        }
        return false;
    }

    private boolean isAdmin(SysUser user) {
        String role = user.getRole();
        return role != null && ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));
    }

    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return StringUtils.hasText(uri) ? uri : "/";
    }

    private void writeJson(HttpServletResponse response, int httpStatus, ApiResult<?> body) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
