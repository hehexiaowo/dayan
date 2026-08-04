package com.dayan.common.core.exception;

/**
 * 资源不存在异常。
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public NotFoundException() {
        super(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }
}
