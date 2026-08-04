package com.dayan.common.core.exception;

/**
 * Token 已过期异常。
 */
public class TokenExpiredException extends BusinessException {

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }

    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED, ErrorCode.TOKEN_EXPIRED.getMessage());
    }
}
