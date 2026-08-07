package com.dayan.common.security.config;

import cn.dev33.satoken.stp.StpUtil;
import com.dayan.common.security.StpKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 默认 StpLogic 初始化器。
 *
 * <p>每个 starter 进程启动时，按 {@code dayan.security.login-type} 把对应端的 StpLogic
 * 设为 {@link StpUtil} 默认实例，使 {@code @SaCheckPermission} 注解与 SaInterceptor
 * 默认 {@code checkLogin} 走本端命名空间（admin/channel/agent/...），而非 Sa-Token
 * 内置的 "login" 命名空间。
 *
 * <p>背景：各端登录用 {@link StpKit#CHANNEL} 等独立 loginType 命名空间存会话，
 * 但注解与拦截器默认走 {@link StpUtil}（loginType="login"），命名空间不匹配导致
 * token 查不到、鉴权形同虚设（P9 增量 2 任务 6 冒烟暴露）。
 *
 * <p>前提：每 starter 是独立 JVM 进程且只加载本端 Controller（ComponentScan
 * excludeFilters），进程内当前端唯一，全局覆盖 {@link StpUtil} 无冲突。
 *
 * <p>用 {@link ApplicationRunner}：在 Spring 完全启动、接收请求前执行，
 * 确保 Sa-Token starter 配置已就绪且覆盖早于首个请求。
 */
@Component
public class DefaultStpLogicInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultStpLogicInitializer.class);

    @Value("${dayan.security.login-type:}")
    private String loginType;

    @Override
    public void run(ApplicationArguments args) {
        if (loginType == null || loginType.isBlank()) {
            // 未配置则保持 Sa-Token 默认（StpUtil loginType="login"）
            return;
        }
        StpUtil.setStpLogic(StpKit.of(loginType));
        log.info("[鉴权] 默认 StpLogic 已设为 loginType={}, @SaCheckPermission 将走本端命名空间", loginType);
    }
}
