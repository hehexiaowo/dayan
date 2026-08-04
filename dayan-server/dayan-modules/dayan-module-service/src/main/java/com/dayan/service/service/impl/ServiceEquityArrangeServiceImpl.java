package com.dayan.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ArrangeConfirmDTO;
import com.dayan.service.dto.ServiceEquityArrangeCreateDTO;
import com.dayan.service.dto.ServiceEquityArrangeQueryDTO;
import com.dayan.service.dto.ServiceEquityArrangeUpdateDTO;
import com.dayan.service.entity.ServiceEquityArrange;
import com.dayan.service.mapper.ServiceEquityArrangeMapper;
import com.dayan.service.service.ServiceEquityArrangeService;
import com.dayan.service.vo.ServiceEquityArrangeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全程安排（service_equity_arrange）服务实现。
 *
 * <p>arrangeCode 生成：{@code "AR" + format(%010d, seq)}。
 * arrangeTimeStart < arrangeTimeEnd 校验；isConfirmed 置 1 后方可 start_service。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceEquityArrangeServiceImpl implements ServiceEquityArrangeService {

    private static final String AR_PREFIX = "AR";
    private static final String AR_SEQ_KEY = "code:seq:AR:0";
    private static final int AR_SEQ_WIDTH = 10;

    private final ServiceEquityArrangeMapper arrangeMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<ServiceEquityArrangeVO> page(ServiceEquityArrangeQueryDTO query) {
        LambdaQueryWrapper<ServiceEquityArrange> wrapper = buildWrapper(query);
        Page<ServiceEquityArrange> page = arrangeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ServiceEquityArrangeVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ServiceEquityArrangeVO> listBySession(String sessionCode) {
        return arrangeMapper.selectList(new LambdaQueryWrapper<ServiceEquityArrange>()
                .eq(ServiceEquityArrange::getSessionCode, sessionCode)
                .orderByAsc(ServiceEquityArrange::getArrangeDate)
                .orderByAsc(ServiceEquityArrange::getArrangeTimeStart)
                .orderByDesc(ServiceEquityArrange::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ServiceEquityArrangeVO getDetail(Long id) {
        return toVO(requireArrange(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ServiceEquityArrangeCreateDTO dto) {
        validateTimeRange(dto.getArrangeTimeStart(), dto.getArrangeTimeEnd());

        ServiceEquityArrange entity = new ServiceEquityArrange();
        String arrangeCode = generateArrangeCode();
        entity.setArrangeCode(arrangeCode);
        entity.setSessionCode(dto.getSessionCode());
        entity.setSolutionCode(dto.getSolutionCode());
        entity.setClientCode(dto.getClientCode());
        entity.setButlerCode(dto.getButlerCode());
        entity.setArrangeType(dto.getArrangeType());
        entity.setParkCode(dto.getParkCode());
        entity.setParkFullName(dto.getParkFullName());
        entity.setArrangeDate(dto.getArrangeDate());
        entity.setArrangeTimeStart(dto.getArrangeTimeStart());
        entity.setArrangeTimeEnd(dto.getArrangeTimeEnd());
        entity.setArrangeAddress(dto.getArrangeAddress());
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactPhone(dto.getContactPhone());
        entity.setParticipantCount(dto.getParticipantCount());
        entity.setPrepareItems(dto.getPrepareItems());
        entity.setIsConfirmed(0);
        entity.setStatus(0);
        entity.setRemark(dto.getRemark());
        arrangeMapper.insert(entity);
        log.info("创建安排成功: sessionCode={}, arrangeCode={}", dto.getSessionCode(), arrangeCode);
        return arrangeCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ServiceEquityArrangeUpdateDTO dto) {
        ServiceEquityArrange existing = requireArrange(id);
        validateTimeRange(dto.getArrangeTimeStart(), dto.getArrangeTimeEnd());

        ServiceEquityArrange update = new ServiceEquityArrange();
        update.setId(existing.getId());
        if (dto.getSolutionCode() != null) update.setSolutionCode(dto.getSolutionCode());
        if (dto.getArrangeType() != null) update.setArrangeType(dto.getArrangeType());
        if (dto.getParkCode() != null) update.setParkCode(dto.getParkCode());
        if (dto.getParkFullName() != null) update.setParkFullName(dto.getParkFullName());
        if (dto.getArrangeDate() != null) update.setArrangeDate(dto.getArrangeDate());
        if (dto.getArrangeTimeStart() != null) update.setArrangeTimeStart(dto.getArrangeTimeStart());
        if (dto.getArrangeTimeEnd() != null) update.setArrangeTimeEnd(dto.getArrangeTimeEnd());
        if (dto.getArrangeAddress() != null) update.setArrangeAddress(dto.getArrangeAddress());
        if (dto.getContactPerson() != null) update.setContactPerson(dto.getContactPerson());
        if (dto.getContactPhone() != null) update.setContactPhone(dto.getContactPhone());
        if (dto.getParticipantCount() != null) update.setParticipantCount(dto.getParticipantCount());
        if (dto.getPrepareItems() != null) update.setPrepareItems(dto.getPrepareItems());
        if (dto.getProgressNotes() != null) update.setProgressNotes(dto.getProgressNotes());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getCancelReason() != null) update.setCancelReason(dto.getCancelReason());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        arrangeMapper.updateById(update);
        log.info("更新安排成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(ArrangeConfirmDTO dto) {
        ServiceEquityArrange existing = requireArrange(dto.getId());
        ServiceEquityArrange update = new ServiceEquityArrange();
        update.setId(existing.getId());
        update.setIsConfirmed(dto.getIsConfirmed());
        if (dto.getIsConfirmed() != null && dto.getIsConfirmed() == 1) {
            update.setConfirmTime(LocalDateTime.now());
        }
        arrangeMapper.updateById(update);
        log.info("安排确认: id={}, isConfirmed={}", dto.getId(), dto.getIsConfirmed());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ServiceEquityArrange existing = requireArrange(id);
        arrangeMapper.deleteById(existing.getId());
        log.info("删除安排成功: id={}", id);
    }

    @Override
    public long countByConfirmed(String sessionCode, Integer isConfirmed) {
        Long count = arrangeMapper.selectCount(new LambdaQueryWrapper<ServiceEquityArrange>()
                .eq(ServiceEquityArrange::getSessionCode, sessionCode)
                .eq(ServiceEquityArrange::getIsConfirmed, isConfirmed));
        return count == null ? 0 : count;
    }

    // ====== 内部方法 ======

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间必须早于结束时间");
        }
    }

    private LambdaQueryWrapper<ServiceEquityArrange> buildWrapper(ServiceEquityArrangeQueryDTO query) {
        LambdaQueryWrapper<ServiceEquityArrange> wrapper = new LambdaQueryWrapper<ServiceEquityArrange>()
                .orderByDesc(ServiceEquityArrange::getArrangeDate)
                .orderByDesc(ServiceEquityArrange::getId);
        if (query.getSessionCode() != null && !query.getSessionCode().isEmpty()) {
            wrapper.eq(ServiceEquityArrange::getSessionCode, query.getSessionCode());
        }
        if (query.getArrangeCode() != null && !query.getArrangeCode().isEmpty()) {
            wrapper.eq(ServiceEquityArrange::getArrangeCode, query.getArrangeCode());
        }
        if (query.getSolutionCode() != null && !query.getSolutionCode().isEmpty()) {
            wrapper.eq(ServiceEquityArrange::getSolutionCode, query.getSolutionCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ServiceEquityArrange::getClientCode, query.getClientCode());
        }
        if (query.getButlerCode() != null && !query.getButlerCode().isEmpty()) {
            wrapper.eq(ServiceEquityArrange::getButlerCode, query.getButlerCode());
        }
        if (query.getArrangeType() != null) {
            wrapper.eq(ServiceEquityArrange::getArrangeType, query.getArrangeType());
        }
        if (query.getIsConfirmed() != null) {
            wrapper.eq(ServiceEquityArrange::getIsConfirmed, query.getIsConfirmed());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ServiceEquityArrange::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ServiceEquityArrange requireArrange(Long id) {
        ServiceEquityArrange entity = arrangeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "安排不存在: id=" + id);
        }
        return entity;
    }

    private String generateArrangeCode() {
        long seq = sequenceProvider.next(AR_SEQ_KEY);
        return AR_PREFIX + String.format("%0" + AR_SEQ_WIDTH + "d", seq);
    }

    private ServiceEquityArrangeVO toVO(ServiceEquityArrange entity) {
        ServiceEquityArrangeVO vo = new ServiceEquityArrangeVO();
        vo.setId(entity.getId());
        vo.setSessionCode(entity.getSessionCode());
        vo.setSolutionCode(entity.getSolutionCode());
        vo.setClientCode(entity.getClientCode());
        vo.setButlerCode(entity.getButlerCode());
        vo.setArrangeCode(entity.getArrangeCode());
        vo.setArrangeType(entity.getArrangeType());
        vo.setParkCode(entity.getParkCode());
        vo.setParkFullName(entity.getParkFullName());
        vo.setArrangeDate(entity.getArrangeDate());
        vo.setArrangeTimeStart(entity.getArrangeTimeStart());
        vo.setArrangeTimeEnd(entity.getArrangeTimeEnd());
        vo.setArrangeAddress(entity.getArrangeAddress());
        vo.setContactPerson(entity.getContactPerson());
        vo.setContactPhone(entity.getContactPhone());
        vo.setParticipantCount(entity.getParticipantCount());
        vo.setPrepareItems(entity.getPrepareItems());
        vo.setProgressNotes(entity.getProgressNotes());
        vo.setConfirmTime(entity.getConfirmTime());
        vo.setCompleteTime(entity.getCompleteTime());
        vo.setIsConfirmed(entity.getIsConfirmed());
        vo.setStatus(entity.getStatus());
        vo.setCancelReason(entity.getCancelReason());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
