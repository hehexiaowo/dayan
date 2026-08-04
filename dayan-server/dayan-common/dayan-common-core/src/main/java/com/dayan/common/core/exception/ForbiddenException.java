package com.dayan.common.core.exception;

/**
 * 权限不足异常。
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
    }
}
