package com.example.aiserver.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private int code;
    private String msg;
    private T data;
    private String traceId;

    public ApiResult(int code, String msg, T data) {
        this(code, msg, data, null);
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }

    public static ApiResult<Void> success() {
        return new ApiResult<>(200, "success", null);
    }

    public static ApiResult<Void> fail(String msg) {
        return new ApiResult<>(500, msg, null);
    }

    public static <T> ApiResult<T> unauthorized(String msg) {
        return new ApiResult<>(-1, msg, null);
    }

    public static ApiResult<Void> fail(String msg, String traceId) {
        return new ApiResult<>(500, msg, null, traceId);
    }
}
