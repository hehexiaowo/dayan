package com.dayan.common.log.auth;

/**
 * 认证日志记录器 SPI。
 *
 * <p>登录/登出发生在鉴权之前（或之时），{@code @OperationLog} 切面拿不到账号上下文，
 * 因此由各端认证服务显式调用本接口记录。实现由 system 模块提供
 * （按 accountType 路由落库到对应端的 system_log_* 表）。
 *
 * <p>业务模块经 {@code ObjectProvider<AuthLogRecorder>} 注入使用，
 * 无实现时（如纯工具模块单测）直接跳过，不影响主流程。
 */
public interface AuthLogRecorder {

    /**
     * 记录登录事件（成功或失败）。
     *
     * @param accountType 账号类型（admin/channel/agent/client，见 AccountType.loginType）
     * @param accountCode 账号编码（账号不存在等场景为 null）
     * @param accountName 操作人姓名（可空）
     * @param loginType   登录方式（password/sms/wx）
     * @param identity    登录标识（用户名/手机号，实现方负责脱敏后落库）
     * @param success     是否成功
     * @param failReason  失败原因（成功时传 null）
     */
    void recordLogin(String accountType, String accountCode, String accountName,
                     String loginType, String identity, boolean success, String failReason);

    /**
     * 记录登出事件。
     *
     * @param accountType 账号类型
     * @param accountCode 账号编码
     * @param accountName 操作人姓名（可空）
     */
    void recordLogout(String accountType, String accountCode, String accountName);
}
