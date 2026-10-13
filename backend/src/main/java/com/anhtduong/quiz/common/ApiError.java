package com.anhtduong.quiz.common;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Định dạng lỗi thống nhất: {status, code, message, details, path, timestamp}.
 * Đây là class trả về cho client; {@link BusinessException} sẽ ném để GlobalExceptionHandler
 * chuyển thành response JSON này.
 */
public record ApiError(
        int status,
        String code,
        String message,
        Object details,
        String path,
        Instant timestamp
) {
    public static ApiError of(HttpStatus status, String code, String message, Object details, String path) {
        return new ApiError(status.value(), code, message, details, path, Instant.now());
    }
}
