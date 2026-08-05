package com.dayan.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.service.dto.AssignButlerDTO;
import com.dayan.service.dto.CancelSessionDTO;
import com.dayan.service.dto.ServiceSessionCreateDTO;
import com.dayan.service.dto.ServiceSessionQueryDTO;
import com.dayan.service.dto.ServiceSessionUpdateDTO;
import com.dayan.service.dto.SubStatusUpdateDTO;
import com.dayan.service.dto.TransitionDTO;
import com.dayan.service.entity.ButlerInfoView;
import com.dayan.service.entity.ServiceEquityArrange;
import com.dayan.service.entity.ServiceEquitySolution;
import com.dayan.service.entity.ServiceSession;
import com.dayan.service.enums.ServiceSessionEvent;
import com.dayan.service.event.ServiceSessionFinishedEvent;
import com.dayan.service.event.ServiceSessionStartedEvent;
import com.dayan.service.mapper.ButlerInfoViewMapper;
import com.dayan.service.mapper.ServiceEquityArrangeMapper;
import com.dayan.service.mapper.ServiceEquitySolutionMapper;
import com.dayan.service.mapper.ServiceSessionMapper;
import com.dayan.service.service.ServiceSessionService;
import com.dayan.service.vo.ServiceSessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务会话（service_session）服务实现 —— 服务域核心。
 *
 * <p>核心：7 态状态机（SERVICE_SESSION_SM）+ 子状态管理 + 四环节联动动作。
 * <ul>
 *   <li>session_code：{@code "SS" + format(%010d, seq)}</li>
 *   <li>创建：初始 session_status=1（待分配）、sub_status=normal</li>
 *   <li>状态变更均经 {@link StateMachineEngine#transition}，返回 to 后落库</li>
 *   <li>子状态（sub_status）独立于状态机，由应用层 UPDATE；终态校验：
 *       session_status=6(完成)/7(取消) 且 sub_status=refund_done 时拒绝再转</li>
 *   <li>分配管家：查 butler_info 只读视图填 butlerFullName 快照（不依赖 butler 模块）</li>
 *   <li>四环节联动：confirm_solution 前校验存在 isAccepted=1 方案；
 *       start_service 前校验存在 isConfirmed=1 安排</li>
 * </ul>
 *
 * <p>所有写操作 {@code @Transactional}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceSessionServiceImpl implements ServiceSessionService {

    private static final String SS_PREFIX = "SS";
    private static final String SS_SEQ_KEY = "code:seq:SS:0";
    private static final int SS_SEQ_WIDTH = 10;

    private final ServiceSessionMapper sessionMapper;
    private final ButlerInfoViewMapper butlerInfoViewMapper;
    private final ServiceEquitySolutionMapper solutionMapper;
    private final ServiceEquityArrangeMapper arrangeMapper;
    private final SequenceProvider sequenceProvider;
    private final StateMachineEngine stateMachineEngine;
    private final ApplicationEventPublisher eventPublisher;

    // ====== 查询 ======

    @Override
    public PageResult<ServiceSessionVO> page(ServiceSessionQueryDTO query) {
        LambdaQueryWrapper<ServiceSession> wrapper = buildWrapper(query);
        Page<ServiceSession> page = sessionMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ServiceSessionVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ServiceSessionVO> list(ServiceSessionQueryDTO query) {
        return sessionMapper.selectList(buildWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ServiceSessionVO getDetail(String sessionCode) {
        return toVO(requireSession(sessionCode));
    }

    @Override
    public ServiceSession requireSession(String sessionCode) {
        ServiceSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ServiceSession>()
                .eq(ServiceSession::getSessionCode, sessionCode)
                .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在: " + sessionCode);
        }
        return session;
    }

    // ====== 创建 / 更新 / 删除 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ServiceSessionCreateDTO dto) {
        ServiceSession entity = new ServiceSession();
        String sessionCode = generateSessionCode();
        entity.setSessionCode(sessionCode);
        entity.setEquityCode(dto.getEquityCode());
        entity.setClientCode(dto.getClientCode());
        entity.setServiceType(dto.getServiceType());
        entity.setServiceTitle(dto.getServiceTitle());
        entity.setServiceDescription(dto.getServiceDescription());
        entity.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        entity.setSourceType(dto.getSourceType() == null ? 1 : dto.getSourceType());
        entity.setSourceCode(dto.getSourceCode());
        entity.setParkCode(dto.getParkCode());
        entity.setParkFullName(dto.getParkFullName());
        entity.setAgentCode(dto.getAgentCode());
        entity.setChannelCode(dto.getChannelCode());
        entity.setTouchCount(0);
        // 初始状态：待分配 + normal
        entity.setSessionStatus(ServiceSessionEvent.STATUS_PENDING_ASSIGN);
        entity.setSubStatus(ServiceSessionEvent.SUB_NORMAL);
        entity.setRemark(dto.getRemark());
        sessionMapper.insert(entity);
        log.info("创建服务会话成功: sessionCode={}, clientCode={}", sessionCode, dto.getClientCode());
        return sessionCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String sessionCode, ServiceSessionUpdateDTO dto) {
        ServiceSession existing = requireSession(sessionCode);
        ServiceSession update = new ServiceSession();
        update.setId(existing.getId());
        if (dto.getServiceTitle() != null) update.setServiceTitle(dto.getServiceTitle());
        if (dto.getServiceDescription() != null) update.setServiceDescription(dto.getServiceDescription());
        if (dto.getPriority() != null) update.setPriority(dto.getPriority());
        if (dto.getParkCode() != null) update.setParkCode(dto.getParkCode());
        if (dto.getParkFullName() != null) update.setParkFullName(dto.getParkFullName());
        if (dto.getAgentCode() != null) update.setAgentCode(dto.getAgentCode());
        if (dto.getChannelCode() != null) update.setChannelCode(dto.getChannelCode());
        if (dto.getIsSatisfied() != null) update.setIsSatisfied(dto.getIsSatisfied());
        if (dto.getOverallRating() != null) update.setOverallRating(dto.getOverallRating());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        sessionMapper.updateById(update);
        log.info("更新服务会话成功: sessionCode={}", sessionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String sessionCode) {
        ServiceSession existing = requireSession(sessionCode);
        sessionMapper.deleteById(existing.getId());
        log.info("删除服务会话成功: sessionCode={}", sessionCode);
    }

    // ====== 状态机业务动作（封装事件） ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignButler(AssignButlerDTO dto) {
        ServiceSession session = requireSession(dto.getSessionCode());
        checkTerminal(session);

        // 查 butler 只读视图，校验在职 + 取全名快照
        ButlerInfoView butler = butlerInfoViewMapper.selectOne(new LambdaQueryWrapper<ButlerInfoView>()
                .eq(ButlerInfoView::getButlerCode, dto.getButlerCode())
                .last("LIMIT 1"));
        if (butler == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家不存在: " + dto.getButlerCode());
        }
        if (butler.getStatus() == null || butler.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "管家非在职状态，无法分配: " + dto.getButlerCode());
        }

        int from = currentStatus(session);
        int to = stateMachineEngine.transition(ServiceSessionEvent.DOMAIN, from, ServiceSessionEvent.ASSIGN_BUTLER);

        ServiceSession update = new ServiceSession();
        update.setId(session.getId());
        update.setSessionStatus(to);
        update.setButlerCode(butler.getButlerCode());
        update.setButlerFullName(butler.getFullName());
        update.setAcceptTime(LocalDateTime.now());
        sessionMapper.updateById(update);
        log.info("分配管家成功: sessionCode={}, butlerCode={}, from={}, to={}",
                dto.getSessionCode(), dto.getButlerCode(), from, to);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDemand(String sessionCode) {
        ServiceSession session = requireSession(sessionCode);
        checkTerminal(session);
        int from = currentStatus(session);
        int to = stateMachineEngine.transition(ServiceSessionEvent.DOMAIN, from, ServiceSessionEvent.SUBMIT_DEMAND);
        applyStatus(session, to);
        log.info("提交需求: sessionCode={}, from={}, to={}", sessionCode, from, to);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSolution(String sessionCode) {
        ServiceSession session = requireSession(sessionCode);
        checkTerminal(session);
        // 校验存在 isAccepted=1 的方案
        Long accepted = solutionMapper.selectCount(new LambdaQueryWrapper<ServiceEquitySolution>()
                .eq(ServiceEquitySolution::getSessionCode, sessionCode)
                .eq(ServiceEquitySolution::getIsAccepted, 1));
        if (accepted == null || accepted == 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "无已接受的方案，无法确认方案: " + sessionCode);
        }
        int from = currentStatus(session);
        int to = stateMachineEngine.transition(ServiceSessionEvent.DOMAIN, from, ServiceSessionEvent.CONFIRM_SOLUTION);
        applyStatus(session, to);
        log.info("确认方案: sessionCode={}, from={}, to={}", sessionCode, from, to);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSolution(String sessionCode) {
        ServiceSession session = requireSession(sessionCode);
        checkTerminal(session);
        int from = currentStatus(session);
        int to = stateMachineEngine.transition(ServiceSessionEvent.DOMAIN, from, ServiceSessionEvent.REJECT_SOLUTION);

        // 方案 adjustCount +1（会话下所有方案）
        List<ServiceEquitySolution> solutions = solutionMapper.selectList(
                new LambdaQueryWrapper<ServiceEquitySolution>()
                        .eq(ServiceEquitySolution::getSessionCode, sessionCode));
        for (ServiceEquitySolution sol : solutions) {
            ServiceEquitySolution upd = new ServiceEquitySolution();
            upd.setId(sol.getId());
            upd.setAdjustCount((sol.getAdjustCount() == null ? 0 : sol.getAdjustCount()) + 1);
            solutionMapper.updateById(upd);
        }
        applyStatus(session, to);
        log.info("驳回方案: sessionCode={}, from={}, to={}, adjustSolutions={}",
                sessionCode, from, to, solutions.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startService(String sessionCode) {
        ServiceSession session = requireSession(sessionCode);
        checkTerminal(session);
        // 校验存在 isConfirmed=1 的安排
        Long confirmed = arrangeMapper.selectCount(new LambdaQueryWrapper<ServiceEquityArrange>()
                .eq(ServiceEquityArrange::getSessionCode, sessionCode)
                .eq(ServiceEquityArrange::getIsConfirmed, 1));
        if (confirmed == null || confirmed == 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "无已确认的安排，无法开始服务: " + sessionCode);
        }
        int from = currentStatus(session);
        int to = stateMachineEngine.transition(ServiceSessionEvent.DOMAIN, from, ServiceSessionEvent.START_SERVICE);
        applyStatus(session, to);
        log.info("开始服务: sessionCode={}, from={}, to={}", sessionCode, from, to);
        // 联动权益状态：发布事件，由 equity 模块监听（2→3 使用中 + use_count 维护）
        try {
            eventPublisher.publishEvent(new ServiceSessionStartedEvent(
                    this, sessionCode, session.getEquityCode(), session.getClientCode()));
        } catch (Exception e) {
            log.warn("发布服务开始事件失败（不影响主流程）: sessionCode={}", sessionCode, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(String sessionCode) {
        ServiceSession session = requireSession(sessionCode);
        checkTerminal(session);
        int from = currentStatus(session);
        int to = stateMachineEngine.transition(ServiceSessionEvent.DOMAIN, from, ServiceSessionEvent.FINISH);
        ServiceSession update = new ServiceSession();
        update.setId(session.getId());
        update.setSessionStatus(to);
        update.setCompleteTime(LocalDateTime.now());
        sessionMapper.updateById(update);
        log.info("完成服务: sessionCode={}, from={}, to={}", sessionCode, from, to);
        // 联动权益使用计数：发布事件，由 equity 模块监听（避免 service→equity 循环依赖）
        try {
            eventPublisher.publishEvent(new ServiceSessionFinishedEvent(
                    this, sessionCode, session.getEquityCode(), session.getClientCode()));
        } catch (Exception e) {
            log.warn("发布服务完成事件失败（不影响主流程）: sessionCode={}", sessionCode, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(CancelSessionDTO dto) {
        ServiceSession session = requireSession(dto.getSessionCode());
        checkTerminal(session);
        int from = currentStatus(session);
        int to = stateMachineEngine.transition(ServiceSessionEvent.DOMAIN, from, ServiceSessionEvent.CANCEL);
        ServiceSession update = new ServiceSession();
        update.setId(session.getId());
        update.setSessionStatus(to);
        update.setCloseReason(dto.getCloseReason());
        update.setCloseTime(LocalDateTime.now());
        sessionMapper.updateById(update);
        log.info("取消会话: sessionCode={}, from={}, to={}, reason={}",
                dto.getSessionCode(), from, to, dto.getCloseReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer transition(TransitionDTO dto) {
        ServiceSession session = requireSession(dto.getSessionCode());
        checkTerminal(session);
        int from = currentStatus(session);
        int to = stateMachineEngine.transition(ServiceSessionEvent.DOMAIN, from, dto.getEvent());
        applyStatus(session, to);
        log.info("会话状态流转: sessionCode={}, {} --{}--> {}", dto.getSessionCode(), from, dto.getEvent(), to);
        return to;
    }

    // ====== 子状态管理（独立于状态机） ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubStatus(SubStatusUpdateDTO dto) {
        ServiceSession session = requireSession(dto.getSessionCode());
        // 终态校验：完成/取消且 refund_done 时拒绝再转
        if (isTerminalStatus(session.getSessionStatus())
                && ServiceSessionEvent.SUB_REFUND_DONE.equals(session.getSubStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "会话已终态且退款完成，子状态不可再变更: " + dto.getSessionCode());
        }
        sessionMapper.update(null, new LambdaUpdateWrapper<ServiceSession>()
                .eq(ServiceSession::getId, session.getId())
                .set(ServiceSession::getSubStatus, dto.getSubStatus()));
        log.info("更新子状态: sessionCode={}, subStatus={}", dto.getSessionCode(), dto.getSubStatus());
    }

    // ====== 内部方法 ======

    /** 当前 sessionStatus，空值兜底为待分配。 */
    private int currentStatus(ServiceSession session) {
        return session.getSessionStatus() == null
                ? ServiceSessionEvent.STATUS_PENDING_ASSIGN : session.getSessionStatus();
    }

    /** 应用状态机 to 到会话（仅更新 session_status）。 */
    private void applyStatus(ServiceSession session, int to) {
        ServiceSession update = new ServiceSession();
        update.setId(session.getId());
        update.setSessionStatus(to);
        sessionMapper.updateById(update);
    }

    /** 终态校验：完成/取消且 sub_status=refund_done 时拒绝状态机流转。 */
    private void checkTerminal(ServiceSession session) {
        if (isTerminalStatus(session.getSessionStatus())
                && ServiceSessionEvent.SUB_REFUND_DONE.equals(session.getSubStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "会话已终态且退款完成，不可再流转: " + session.getSessionCode());
        }
    }

    /** 是否终态（完成/取消）。 */
    private boolean isTerminalStatus(Integer status) {
        return status != null && (status == ServiceSessionEvent.STATUS_COMPLETED
                || status == ServiceSessionEvent.STATUS_CANCELLED);
    }

    private LambdaQueryWrapper<ServiceSession> buildWrapper(ServiceSessionQueryDTO query) {
        LambdaQueryWrapper<ServiceSession> wrapper = new LambdaQueryWrapper<ServiceSession>()
                .orderByDesc(ServiceSession::getCreatedAt)
                .orderByDesc(ServiceSession::getId);
        if (query.getSessionCode() != null && !query.getSessionCode().isEmpty()) {
            wrapper.eq(ServiceSession::getSessionCode, query.getSessionCode());
        }
        if (query.getEquityCode() != null && !query.getEquityCode().isEmpty()) {
            wrapper.eq(ServiceSession::getEquityCode, query.getEquityCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ServiceSession::getClientCode, query.getClientCode());
        }
        if (query.getButlerCode() != null && !query.getButlerCode().isEmpty()) {
            wrapper.eq(ServiceSession::getButlerCode, query.getButlerCode());
        }
        if (query.getServiceType() != null) {
            wrapper.eq(ServiceSession::getServiceType, query.getServiceType());
        }
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ServiceSession::getParkCode, query.getParkCode());
        }
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(ServiceSession::getAgentCode, query.getAgentCode());
        }
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(ServiceSession::getChannelCode, query.getChannelCode());
        }
        if (query.getSessionStatus() != null) {
            wrapper.eq(ServiceSession::getSessionStatus, query.getSessionStatus());
        }
        if (query.getSubStatus() != null && !query.getSubStatus().isEmpty()) {
            wrapper.eq(ServiceSession::getSubStatus, query.getSubStatus());
        }
        if (query.getSourceType() != null) {
            wrapper.eq(ServiceSession::getSourceType, query.getSourceType());
        }
        return wrapper;
    }

    private String generateSessionCode() {
        long seq = sequenceProvider.next(SS_SEQ_KEY);
        return SS_PREFIX + String.format("%0" + SS_SEQ_WIDTH + "d", seq);
    }

    private ServiceSessionVO toVO(ServiceSession entity) {
        ServiceSessionVO vo = new ServiceSessionVO();
        vo.setId(entity.getId());
        vo.setSessionCode(entity.getSessionCode());
        vo.setEquityCode(entity.getEquityCode());
        vo.setClientCode(entity.getClientCode());
        vo.setButlerCode(entity.getButlerCode());
        vo.setButlerFullName(entity.getButlerFullName());
        vo.setServiceType(entity.getServiceType());
        vo.setServiceTitle(entity.getServiceTitle());
        vo.setServiceDescription(entity.getServiceDescription());
        vo.setPriority(entity.getPriority());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceCode(entity.getSourceCode());
        vo.setParkCode(entity.getParkCode());
        vo.setParkFullName(entity.getParkFullName());
        vo.setAgentCode(entity.getAgentCode());
        vo.setChannelCode(entity.getChannelCode());
        vo.setAcceptTime(entity.getAcceptTime());
        vo.setCompleteTime(entity.getCompleteTime());
        vo.setCloseTime(entity.getCloseTime());
        vo.setTotalDuration(entity.getTotalDuration());
        vo.setTouchCount(entity.getTouchCount());
        vo.setIsSatisfied(entity.getIsSatisfied());
        vo.setOverallRating(entity.getOverallRating());
        vo.setSessionStatus(entity.getSessionStatus());
        vo.setSubStatus(entity.getSubStatus());
        vo.setCloseReason(entity.getCloseReason());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
