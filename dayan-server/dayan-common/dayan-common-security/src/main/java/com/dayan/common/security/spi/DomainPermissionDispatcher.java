package com.dayan.common.security.spi;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 多端 Sa-Token 权限解析总入口（全局唯一 {@code StpInterface} bean）。
 *
 * <p>本类是 Sa-Token 在 {@code @SaCheckPermission} / {@code @SaCheckRole} 校验时的唯一回调点。
 * 它不做具体权限查询，而是按 {@code loginType} 将请求分发到对应业务域的
 * {@link DomainPermissionResolver} 实例（organ/channel/agent/...，由各 module 提供）。
 *
 * <p><b>为何要 dispatcher：</b>见 {@link DomainPermissionResolver} 的历史背景说明——
 * Sa-Token 1.39 不允许多个 {@code StpInterface} bean 共存，故以单一 dispatcher 收敛入口，
 * 各域解析器作为策略 bean 由 Spring 自动收集（{@code List<DomainPermissionResolver>}）。
 *
 * <p>各 resolver 的加载顺序由 Spring 决定；{@link #findResolver(String)} 取首个支持者，
 * 正常情况下每个 loginType 只有一个 resolver 声明支持。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainPermissionDispatcher implements StpInterface {

    /** 由 Spring 自动收集的全部业务域解析器（可能为空列表，典型含 organ/channel 两个） */
    private final List<DomainPermissionResolver> resolvers;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        DomainPermissionResolver resolver = findResolver(loginType);
        if (resolver == null) {
            // 无 resolver 支持该端：返回空权限，等价于该端无 @SaCheckPermission 权限
            return Collections.emptyList();
        }
        if (loginId == null) {
            return Collections.emptyList();
        }
        return resolver.getPermissionList(loginId.toString());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        DomainPermissionResolver resolver = findResolver(loginType);
        if (resolver == null) {
            return Collections.emptyList();
        }
        if (loginId == null) {
            return Collections.emptyList();
        }
        return resolver.getRoleList(loginId.toString());
    }

    /**
     * 按 loginType 查找首个声明支持的解析器。
     *
     * @param loginType Sa-Token 调用方传入的 loginType（如 "admin"/"channel"/"login"）
     * @return 匹配的解析器；无匹配返回 {@code null}
     */
    private DomainPermissionResolver findResolver(String loginType) {
        if (loginType == null) {
            return null;
        }
        for (DomainPermissionResolver resolver : resolvers) {
            List<String> supported = resolver.supportLoginTypes();
            if (supported != null && supported.contains(loginType)) {
                return resolver;
            }
        }
        return null;
    }
}
