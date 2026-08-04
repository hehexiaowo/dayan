package com.dayan.common.core.exception;

/**
 * 参数校验异常。
 */
public class ParamException extends BusinessException {

    public ParamException(String message) {
        super(ErrorCode.PARAM_ERROR, message);
    }
}
