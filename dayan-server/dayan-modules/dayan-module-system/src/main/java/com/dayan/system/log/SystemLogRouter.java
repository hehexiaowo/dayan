package com.dayan.system.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.system.entity.SystemLogAgent;
import com.dayan.system.entity.SystemLogChannel;
import com.dayan.system.entity.SystemLogClient;
import com.dayan.system.entity.SystemLogEntry;
import com.dayan.system.entity.SystemLogOrgan;
import com.dayan.system.enums.SystemLogSource;
import com.dayan.system.mapper.SystemLogAgentMapper;
import com.dayan.system.mapper.SystemLogChannelMapper;
import com.dayan.system.mapper.SystemLogClientMapper;
import com.dayan.system.mapper.SystemLogOrganMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 系统日志路由：按日志来源把日志实体路由到对应端的 system_log_* 表。
 *
 * <p>四表同构（{@link SystemLogEntry}），本类统一承担映射查找与异步落库，
 * 供 {@link SystemLogPublisher}（@OperationLog 切面）与
 * {@link SystemAuthLogRecorder}（登录/登出）共用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemLogRouter {

    private final SystemLogOrganMapper organMapper;
    private final SystemLogChannelMapper channelMapper;
    private final SystemLogAgentMapper agentMapper;
    private final SystemLogClientMapper clientMapper;

    /** 按来源取对应表的 mapper */
    public BaseMapper<? extends SystemLogEntry> mapperOf(SystemLogSource source) {
        return switch (source) {
            case ORGAN -> organMapper;
            case CHANNEL -> channelMapper;
            case AGENT -> agentMapper;
            case CLIENT -> clientMapper;
        };
    }

    /** 按来源新建对应表的实体 */
    public SystemLogEntry newEntry(SystemLogSource source) {
        return switch (source) {
            case ORGAN -> new SystemLogOrgan();
            case CHANNEL -> new SystemLogChannel();
            case AGENT -> new SystemLogAgent();
            case CLIENT -> new SystemLogClient();
        };
    }

    /**
     * 异步落库（失败仅告警，不影响主流程）。
     * 需在启动模块启用 @EnableAsync（四个启动类均已启用）。
     */
    @Async
    public void save(SystemLogEntry entry) {
        try {
            @SuppressWarnings("unchecked")
            BaseMapper<SystemLogEntry> mapper = (BaseMapper<SystemLogEntry>) mapperOf(entry.logSource());
            mapper.insert(entry);
        } catch (Exception e) {
            log.warn("系统日志落库失败: source={}, module={}, action={}, err={}",
                    entry.logSource(), entry.getModule(), entry.getAction(), e.getMessage());
        }
    }
}
