package com.dayan.system.log;

import com.dayan.common.log.auth.AuthLogRecorder;
import com.dayan.common.log.sensitive.SensitiveUtil;
import com.dayan.common.log.util.UaParser;
import com.dayan.system.entity.SystemLogEntry;
import com.dayan.system.enums.SystemLogSource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证日志记录器实现（system 域）。
 *
 * <p>登录/登出由各端认证服务在鉴权前后显式调用（此时 {@code @OperationLog} 切面
 * 拿不到账号上下文）。按 accountType 经 {@link SystemLogRouter} 路由到对应端的
 * system_log_* 表，统一 module='auth'。
 *
 * <p>IP/UA 等请求信息在调用方线程（请求线程）同步解析完毕后再异步落库，
 * 避免异步线程拿不到 RequestContext。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemAuthLogRecorder implements AuthLogRecorder {

    private final SystemLogRouter router;

    @Override
    public void recordLogin(String accountType, String accountCode, String accountName,
                            String loginType, String identity, boolean success, String failReason) {
        try {
            SystemLogEntry entry = newEntry(accountType, accountCode, accountName, actionOfLoginType(loginType));
            entry.setActionDescription("登录" + (success ? "成功" : "失败"));
            entry.setRequestParams(buildContent(loginType, identity));
            entry.setResultStatus(success ? 1 : 0);
            entry.setErrorMsg(failReason);
            router.save(entry);
        } catch (Exception e) {
            log.warn("登录日志记录失败: accountType={}, accountCode={}, err={}", accountType, accountCode, e.getMessage());
        }
    }

    @Override
    public void recordLogout(String accountType, String accountCode, String accountName) {
        try {
            SystemLogEntry entry = newEntry(accountType, accountCode, accountName, "登出");
            entry.setActionDescription("登出");
            entry.setResultStatus(1);
            router.save(entry);
        } catch (Exception e) {
            log.warn("登出日志记录失败: accountType={}, accountCode={}, err={}", accountType, accountCode, e.getMessage());
        }
    }

    /** loginType → 动作文案（区分登录方式；未知类型兜底"登录"） */
    private String actionOfLoginType(String loginType) {
        if (loginType == null) {
            return "登录";
        }
        switch (loginType) {
            case "password":
                return "密码登录";
            case "sms":
                return "验证码登录";
            case "wx":
                return "微信认证";
            default:
                return "登录";
        }
    }

    /** 在请求线程内构建日志实体（含 IP/UA 解析），随后由 router 异步落库 */
    private SystemLogEntry newEntry(String accountType, String accountCode, String accountName, String action) {
        SystemLogSource source = SystemLogSource.fromAccountType(accountType);
        SystemLogEntry entry = router.newEntry(source);
        entry.setTraceId(MDC.get("traceId"));
        entry.setModule("auth");
        entry.setAction(action);
        entry.setAccountType(accountType != null && !accountType.isBlank() ? accountType : "unknown");
        entry.setAccountCode(accountCode != null && !accountCode.isBlank() ? accountCode : "unknown");
        entry.setAccountName(accountName);
        entry.setTargetType("account");
        entry.setTargetCode(accountCode);
        fillRequest(entry);
        return entry;
    }

    private void fillRequest(SystemLogEntry entry) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        entry.setRequestUrl(request.getRequestURI());
        entry.setRequestMethod(request.getMethod());
        entry.setIpAddress(extractIp(request));
        String ua = request.getHeader("User-Agent");
        entry.setUserAgent(ua);
        entry.setDeviceType(UaParser.deviceType(ua));
        entry.setOs(UaParser.os(ua));
        entry.setBrowser(UaParser.browser(ua));
    }

    /** 登录上下文 JSON：loginType + 脱敏后的登录标识 */
    private String buildContent(String loginType, String identity) {
        String masked = identity != null ? SensitiveUtil.phone(identity) : null;
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"loginType\":\"").append(loginType != null ? loginType : "password").append("\"");
        if (masked != null) {
            sb.append(",\"identity\":\"").append(masked).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 提取客户端真实 IP：依次尝试 X-Forwarded-For / X-Real-IP / Proxy-Client-IP，
     * 取第一个非 unknown 的值；兜底 request.getRemoteAddr()。
     */
    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isUnknownIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isUnknownIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isUnknownIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isUnknownIp(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private boolean isUnknownIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}
