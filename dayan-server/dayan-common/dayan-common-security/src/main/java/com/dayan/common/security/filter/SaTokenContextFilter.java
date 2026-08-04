package com.dayan.common.security.filter;

import cn.dev33.satoken.stp.StpLogic;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.common.security.AccountType;
import com.dayan.common.security.StpKit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sa-Token 上下文过滤器。
 *
 * <p>对每个请求，按四端 StpLogic 检测是否已登录，若登录则将：
 * <ul>
 *   <li>account_code - 写入 {@link ContextHolder}（供自动填充 creator/updater）</li>
 *   <li>account_type - 写入 {@link ContextHolder}</li>
 *   <li>channel_code - 从 Session 读取写入 {@link ContextHolder}（供租户隔离）</li>
 * </ul>
 *
 * <p>请求结束清理 ThreadLocal，避免线程池串号。
 * 过滤器顺序在 Sa-Token 拦截器之后（Sa-Token 默认 Order 较低），确保 Token 已解析。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class SaTokenContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            fillContext();
            filterChain.doFilter(request, response);
        } finally {
            ContextHolder.clear();
        }
    }

    /**
     * 检测当前请求来自哪一端，并填充上下文。
     * 四端优先级：admin > channel > agent > client > supplier > distributor。
     * 命中第一个已登录端即停止（同一请求不会同时持有两端 Token）。
     */
    private void fillContext() {
        AccountType matched = detectLoginType();
        if (matched == null) {
            return;
        }
        StpLogic logic = StpKit.of(matched.getLoginType());
        Object loginId = logic.getLoginIdDefaultNull();
        if (loginId == null) {
            return;
        }
        ContextHolder.setAccountCode(loginId.toString());
        ContextHolder.setAccountType(matched.getLoginType());

        // channel_code 存于 Session（登录时写入），agent/client 必有，admin/channel/supplier 视情况
        Object channelCode = logic.getSession().get("channelCode");
        if (channelCode != null) {
            ContextHolder.setChannelCode(channelCode.toString());
        }
    }

    private AccountType detectLoginType() {
        // 按端逐一检测 Token 是否存在且有效
        AccountType[] types = {
                AccountType.ADMIN, AccountType.CHANNEL, AccountType.SUPPLIER,
                AccountType.DISTRIBUTOR, AccountType.AGENT, AccountType.CLIENT
        };
        for (AccountType type : types) {
            StpLogic logic = StpKit.of(type.getLoginType());
            if (logic.getTokenValueNotCut() != null) {
                return type;
            }
        }
        return null;
    }
}
