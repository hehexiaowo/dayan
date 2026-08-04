package com.dayan.common.core.exception;

/**
 * 账号已锁定异常。
 */
public class AccountLockedException extends BusinessException {

    public AccountLockedException(String message) {
        super(ErrorCode.ACCOUNT_LOCKED, message);
    }

    public AccountLockedException() {
        super(ErrorCode.ACCOUNT_LOCKED, ErrorCode.ACCOUNT_LOCKED.getMessage());
    }
}
