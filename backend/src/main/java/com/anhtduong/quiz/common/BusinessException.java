package com.anhtduong.quiz.common;

import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ nghiệp vụ: chuyển thẳng thành response lỗi JSON.
 * Mọi service ném {@code BusinessException} khi vi phạm rule nghiệp vụ.
 */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Object details;

    public BusinessException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public BusinessException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode()       { return code; }
    public Object getDetails()    { return details; }
}
