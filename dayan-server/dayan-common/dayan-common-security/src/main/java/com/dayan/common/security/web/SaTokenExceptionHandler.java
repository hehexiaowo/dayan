package com.dayan.common.security.web;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 鉴权异常处理器。
 *
 * <p>Sa-Token 的 {@code @SaCheckPermission} / {@code @SaCheckRole} / {@code @SaCheckLogin}
 * 注解在校验失败时分别抛出 {@link NotPermissionException} / {@link NotRoleException} /
 * {@link NotLoginException}。若不单独处理，这些异常会掉进 {@code GlobalExceptionHandler}
 * 的兜底 {@code handleSystem}，前端只看到"系统内部异常"，无法区分"未登录"和"无权限"。
 *
 * <p>本处理器将这些异常映射为语义清晰的错误码：
 * <ul>
 *   <li>{@link NotLoginException} → {@link ErrorCode#UNAUTHORIZED}（10101 未登录）</li>
 *   <li>{@link NotPermissionException} / {@link NotRoleException} → {@link ErrorCode#FORBIDDEN}（10200 权限不足）</li>
 * </ul>
 *
 * <p>使用 {@code @Order(Ordered.HIGHEST_PRECEDENCE)} 确保优先于 {@code GlobalExceptionHandler}
 * 的兜底 Exception 处理器生效。
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SaTokenExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SaTokenExceptionHandler.class);

    /** 未登录或 Token 无效 */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLogin(NotLoginException e) {
        log.warn("Sa-Token 未登录: type={}, msg={}", e.getType(), e.getMessage());
        return R.fail(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
    }

    /** 权限不足（@SaCheckPermission 校验失败） */
    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermission(NotPermissionException e) {
        log.warn("Sa-Token 权限不足: need={}, loginType={}", e.getPermission(), e.getLoginType());
        return R.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
    }

    /** 角色不足（@SaCheckRole 校验失败） */
    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRole(NotRoleException e) {
        log.warn("Sa-Token 角色不足: need={}, loginType={}", e.getRole(), e.getLoginType());
        return R.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
    }
}
