package com.example.aiserver.common;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: traceId={}, request={} {}, message={}",
                traceId(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ApiResult.fail(ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResult<Void> handleValidationException(Exception ex, HttpServletRequest request) {
        log.warn("Validation exception: traceId={}, request={} {}",
                traceId(), request.getMethod(), request.getRequestURI(), ex);
        return ApiResult.fail("请求参数不合法", traceId());
    }

    @ExceptionHandler({JwtException.class, NumberFormatException.class})
    public ApiResult<Void> handleJwtException(Exception ex, HttpServletRequest request) {
        log.warn("Token exception: traceId={}, request={} {}",
                traceId(), request.getMethod(), request.getRequestURI(), ex);
        return ApiResult.unauthorized("登录状态已失效，请重新登录");
    }

    @ExceptionHandler({CannotGetJdbcConnectionException.class, DataAccessException.class})
    public ApiResult<Void> handleDatabaseException(Exception ex, HttpServletRequest request) {
        log.error("Database exception: traceId={}, request={} {}, query={}",
                traceId(), request.getMethod(), request.getRequestURI(), request.getQueryString(), ex);
        return ApiResult.fail("数据库访问失败，请检查数据库配置或表结构，请求编号：" + traceId(), traceId());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server exception: traceId={}, request={} {}, query={}",
                traceId(), request.getMethod(), request.getRequestURI(), request.getQueryString(), ex);
        return ApiResult.fail("服务器内部错误，请求编号：" + traceId(), traceId());
    }

    private String traceId() {
        String traceId = MDC.get(RequestTraceFilter.TRACE_ID_KEY);
        return traceId == null ? "unknown" : traceId;
    }
}
