package com.example.aiserver.common;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ApiResult.fail(ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResult<Void> handleValidationException(Exception ex) {
        log.warn("Validation exception", ex);
        return ApiResult.fail("请求参数不合法");
    }

    @ExceptionHandler({JwtException.class, NumberFormatException.class})
    public ApiResult<Void> handleJwtException(Exception ex) {
        log.warn("Token exception", ex);
        return ApiResult.unauthorized("登录状态已失效，请重新登录");
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception ex) {
        log.error("Unhandled server exception", ex);
        return ApiResult.fail("服务器内部错误");
    }
}
