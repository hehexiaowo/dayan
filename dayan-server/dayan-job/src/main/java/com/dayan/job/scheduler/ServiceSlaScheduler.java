package com.dayan.job.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.service.dto.SubStatusUpdateDTO;
import com.dayan.service.entity.ServiceEquityFollowup;
import com.dayan.service.entity.ServiceSession;
import com.dayan.service.enums.ServiceSessionEvent;
import com.dayan.service.mapper.ServiceEquityFollowupMapper;
import com.dayan.service.mapper.ServiceSessionMapper;
import com.dayan.service.service.ServiceSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务会话 SLA 与回访定时任务。
 *
 * <p>每小时执行两类检查：
 * <ul>
 *   <li><b>SLA 超时升级</b>：会话处于"待分配"（status=1）且创建超过 2 小时未受理，
 *       子状态由 {@code normal} 升级为 {@code urgent}，提示运营优先处理。</li>
 *   <li><b>7 天回访校验</b>：会话已完成（status=6）且完成超过 7 天，但无任何回访记录，
 *       记录告警日志（回访缺失由运营跟进，不强制阻塞业务）。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceSlaScheduler {

    /** 待分配会话的 SLA 阈值（小时）：超时未受理则升级为紧急 */
    private static final int SLA_PENDING_HOURS = 2;
    /** 完成后须回访的期限（天） */
    private static final int FOLLOWUP_DEADLINE_DAYS = 7;
    /** 单批处理条数 */
    private static final int BATCH_SIZE = 500;

    private final ServiceSessionMapper serviceSessionMapper;
    private final ServiceEquityFollowupMapper serviceEquityFollowupMapper;
    private final ServiceSessionService serviceSessionService;

    /**
     * 每小时整点执行。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scan() {
        long start = System.currentTimeMillis();
        int slaUpgraded = scanSlaTimeout();
        int followupMissing = scanFollowupMissing();
        log.info("[服务SLA扫描] SLA升级 {} 条、回访缺失告警 {} 条，耗时 {}ms",
                slaUpgraded, followupMissing, System.currentTimeMillis() - start);
    }

    /**
     * SLA 超时升级：待分配(status=1) 且创建超 2 小时 且 sub_status=normal → urgent。
     */
    private int scanSlaTimeout() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(SLA_PENDING_HOURS);
        int upgraded = 0;
        while (true) {
            List<ServiceSession> list = serviceSessionMapper.selectList(
                    new LambdaQueryWrapper<ServiceSession>()
                            .eq(ServiceSession::getSessionStatus, ServiceSessionEvent.STATUS_PENDING_ASSIGN)
                            .eq(ServiceSession::getSubStatus, ServiceSessionEvent.SUB_NORMAL)
                            .lt(ServiceSession::getCreatedAt, threshold)
                            .last("LIMIT " + BATCH_SIZE));
            if (list.isEmpty()) {
                break;
            }
            for (ServiceSession session : list) {
                try {
                    SubStatusUpdateDTO dto = new SubStatusUpdateDTO();
                    dto.setSessionCode(session.getSessionCode());
                    dto.setSubStatus(ServiceSessionEvent.SUB_URGENT);
                    serviceSessionService.updateSubStatus(dto);
                    upgraded++;
                } catch (Exception e) {
                    log.warn("[服务SLA扫描] sessionCode={} 升级紧急失败: {}",
                            session.getSessionCode(), e.getMessage());
                }
            }
            if (list.size() < BATCH_SIZE) {
                break;
            }
        }
        return upgraded;
    }

    /**
     * 7 天回访校验：已完成(status=6) 且完成超 7 天，无回访记录 → 告警日志。
     *
     * <p>仅记录告警，不修改会话状态（回访缺失由运营人工跟进）。
     */
    private int scanFollowupMissing() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(FOLLOWUP_DEADLINE_DAYS);
        int missing = 0;
        while (true) {
            List<ServiceSession> list = serviceSessionMapper.selectList(
                    new LambdaQueryWrapper<ServiceSession>()
                            .eq(ServiceSession::getSessionStatus, ServiceSessionEvent.STATUS_COMPLETED)
                            .isNotNull(ServiceSession::getCompleteTime)
                            .lt(ServiceSession::getCompleteTime, threshold)
                            .last("LIMIT " + BATCH_SIZE));
            if (list.isEmpty()) {
                break;
            }
            for (ServiceSession session : list) {
                Long followupCount = serviceEquityFollowupMapper.selectCount(
                        new LambdaQueryWrapper<ServiceEquityFollowup>()
                                .eq(ServiceEquityFollowup::getSessionCode, session.getSessionCode()));
                if (followupCount == null || followupCount == 0) {
                    log.warn("[服务回访校验] sessionCode={} 已完成超 {} 天但无回访记录，请运营跟进",
                            session.getSessionCode(), FOLLOWUP_DEADLINE_DAYS);
                    missing++;
                }
            }
            if (list.size() < BATCH_SIZE) {
                break;
            }
        }
        return missing;
    }
}
