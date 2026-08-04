package com.dayan.common.core.exception;

import lombok.Getter;

/**
 * 业务异常基类。所有可预期的业务错误均抛本类或其子类，
 * 由 {@code GlobalExceptionHandler} 统一捕获后包装为 {@code R<Void>}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }
}
