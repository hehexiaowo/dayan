package com.dayan.common.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码。
 *
 * <p>采用 5 位数字分段，按业务域/异常类型划分，便于前端按段统一处理。
 * 段位约定见《项目开发规范》§1.4：
 * <ul>
 *   <li>10000 段 - 参数校验</li>
 *   <li>10100 段 - 认证（Token 过期/无效）</li>
 *   <li>10200 段 - 授权（权限不足/账号锁定）</li>
 *   <li>10300 段 - 资源不存在</li>
 *   <li>10400 段 - 通用业务异常</li>
 *   <li>10500 段 - 系统内部异常（兜底）</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    /** 参数校验异常 */
    PARAM_ERROR(10000, "参数校验异常"),
    /** Token 已过期 */
    TOKEN_EXPIRED(10100, "Token 已过期"),
    /** Token 无效 / 未登录 */
    UNAUTHORIZED(10101, "未登录或 Token 无效"),
    /** 权限不足 */
    FORBIDDEN(10200, "权限不足"),
    /** 账号已锁定 */
    ACCOUNT_LOCKED(10201, "账号已锁定"),
    /** 资源不存在 */
    NOT_FOUND(10300, "资源不存在"),
    /** 通用业务异常 */
    BUSINESS(10400, "业务处理异常"),
    /** 系统内部异常（兜底） */
    SYSTEM_ERROR(10500, "系统内部异常");

    private final int code;
    private final String message;
}
