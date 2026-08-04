package com.dayan.common.core.exception;

/**
 * 未登录 / Token 无效异常。
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage());
    }
}
