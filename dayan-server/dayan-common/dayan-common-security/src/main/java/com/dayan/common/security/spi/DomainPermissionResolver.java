package com.dayan.common.security.spi;

import java.util.List;

/**
 * 业务域权限解析器 SPI。
 *
 * <p>多端 Sa-Token 权限解析的扩展点。各业务域（organ/channel/agent/...）实现本接口，
 * 由 {@link DomainPermissionDispatcher} 作为唯一 {@code StpInterface} bean 统一注入，
 * 按 {@code loginType} 分发到支持该端的解析器实例。
 *
 * <p>历史背景：Sa-Token 1.39 的 {@code SaBeanInject.setStpInterface} 为单值注入，
 * 容器中只能存在一个 {@code @Component StpInterface} bean。若每个业务域各注册一个
 * {@code StpInterface}，启动期即抛 {@code NoUniqueBeanDefinitionException}。
 * 故采用 SPI dispatcher 模式：dispatcher 为唯一 {@code StpInterface} bean，
 * 各域解析器实现本接口（非 StpInterface），由 Spring {@code List<DomainPermissionResolver>}
 * 自动收集，避免多 bean 冲突。
 *
 * <p>新增业务域步骤：在对应 module 内新增一个实现类并标注 {@code @Component} 即可，
 * 无需改动 dispatcher 或其他域。
 */
public interface DomainPermissionResolver {

    /**
     * 返回本解析器支持的 Sa-Token loginType 集合（如 {@code "admin"}、{@code "channel"}）。
     *
     * <p>dispatcher 收到调用时按 loginType 查找首个支持的解析器；无匹配时返回空权限。
     * 兼容 Sa-Token 默认 loginType（如 organ 域兼容 {@code "login"}）在此处一并声明。
     *
     * @return 支持的 loginType 列表（不为空）
     */
    List<String> supportLoginTypes();

    /**
     * 返回指定账号在当前业务域所拥有的权限码集合。
     *
     * <p>调用方 dispatcher 已确保 {@code loginType} 命中 {@link #supportLoginTypes()}，
     * 且 {@code loginId} 非空。实现可不做 loginType/loginId 防御。
     *
     * @param accountCode 账号编码（{@code loginId.toString()}）
     * @return 权限码集合；无权限时返回空 List
     */
    List<String> getPermissionList(String accountCode);

    /**
     * 返回指定账号在当前业务域所拥有的角色标识集合。
     *
     * @param accountCode 账号编码（{@code loginId.toString()}）
     * @return 角色标识集合；无角色时返回空 List
     */
    List<String> getRoleList(String accountCode);
}
