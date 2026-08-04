package com.dayan.system.statemachine;

import com.dayan.common.redis.statemachine.DefaultStateMachineEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 状态机规则预热 Runner。
 *
 * <p>应用启动后自动调用 {@link DefaultStateMachineEngine#loadAllRules()}，
 * 把 system_state_machine 表的全部启用规则加载到 Redis Hash 缓存。
 *
 * <p>仅在 dayan-admin / dayan-channel 等引入 system 模块的启动模块生效（@Component 被 ComponentScan 扫到即触发）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StateMachineWarmUpRunner implements ApplicationRunner {

    private final DefaultStateMachineEngine engine;

    @Override
    public void run(ApplicationArguments args) {
        try {
            engine.loadAllRules();
            log.info("[状态机预热] 完成");
        } catch (Exception e) {
            // 预热失败不阻断启动（如 Redis 未就绪），后续首次调用会触发懒加载
            log.warn("[状态机预热] 失败，将在首次调用时懒加载: {}", e.getMessage());
        }
    }
}
