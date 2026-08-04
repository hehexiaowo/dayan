package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerScheduleCreateDTO;
import com.dayan.butler.dto.ButlerScheduleQueryDTO;
import com.dayan.butler.dto.ButlerScheduleUpdateDTO;
import com.dayan.butler.entity.ButlerSchedule;
import com.dayan.butler.mapper.ButlerScheduleMapper;
import com.dayan.butler.service.ButlerScheduleService;
import com.dayan.butler.vo.ButlerScheduleVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 管家排班服务实现。
 *
 * <p><b>基础时间重叠检测</b>：create/update 时查同 butlerCode + 同 scheduleDate 下 status=1 的已有排班，
 * 逐条判断时间段是否重叠——当 {@code newStart < existEnd && newEnd > existStart} 时视为重叠，
 * 重叠抛 BusinessException("排班时间与已有排班冲突")。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerScheduleServiceImpl implements ButlerScheduleService {

    /** 有效状态 */
    private static final int STATUS_ACTIVE = 1;

    private final ButlerScheduleMapper butlerScheduleMapper;

    @Override
    public PageResult<ButlerScheduleVO> page(ButlerScheduleQueryDTO query) {
        LambdaQueryWrapper<ButlerSchedule> wrapper = buildQueryWrapper(query);
        Page<ButlerSchedule> page = butlerScheduleMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerScheduleVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerScheduleVO> list(ButlerScheduleQueryDTO query) {
        LambdaQueryWrapper<ButlerSchedule> wrapper = buildQueryWrapper(query);
        return butlerScheduleMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerScheduleVO getDetail(Long id) {
        return toVO(requireSchedule(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ButlerScheduleCreateDTO dto) {
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        // 重叠检测
        checkOverlap(dto.getButlerCode(), dto.getScheduleDate(),
                dto.getStartTime(), dto.getEndTime(), null);

        ButlerSchedule entity = new ButlerSchedule();
        entity.setButlerCode(dto.getButlerCode());
        entity.setScheduleDate(dto.getScheduleDate());
        entity.setScheduleType(dto.getScheduleType());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setStatus(dto.getStatus() == null ? STATUS_ACTIVE : dto.getStatus());

        butlerScheduleMapper.insert(entity);
        log.info("创建管家排班成功: id={}, butlerCode={}, date={}",
                entity.getId(), dto.getButlerCode(), dto.getScheduleDate());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ButlerScheduleUpdateDTO dto) {
        ButlerSchedule existing = requireSchedule(id);

        LocalDate newDate = dto.getScheduleDate() != null ? dto.getScheduleDate() : existing.getScheduleDate();
        LocalTime newStart = dto.getStartTime() != null ? dto.getStartTime() : existing.getStartTime();
        LocalTime newEnd = dto.getEndTime() != null ? dto.getEndTime() : existing.getEndTime();

        validateTimeRange(newStart, newEnd);
        // 仅当时间或日期变化、或本次 update 仍保持有效状态时才检测重叠
        Integer targetStatus = dto.getStatus() != null ? dto.getStatus() : existing.getStatus();
        if (targetStatus != null && targetStatus == STATUS_ACTIVE) {
            checkOverlap(existing.getButlerCode(), newDate, newStart, newEnd, id);
        }

        ButlerSchedule update = new ButlerSchedule();
        update.setId(existing.getId());
        update.setScheduleDate(newDate);
        if (dto.getScheduleType() != null) update.setScheduleType(dto.getScheduleType());
        update.setStartTime(newStart);
        update.setEndTime(newEnd);
        update.setStatus(targetStatus);

        butlerScheduleMapper.updateById(update);
        log.info("更新管家排班成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ButlerSchedule existing = requireSchedule(id);
        butlerScheduleMapper.deleteById(existing.getId());
        log.info("删除管家排班成功: id={}", id);
    }

    // ====== 内部方法 ======

    /** 校验时间区间合法性：startTime < endTime */
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上班/下班时间不能为空");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上班时间必须早于下班时间");
        }
    }

    /**
     * 重叠检测：查同 butlerCode + scheduleDate 下 status=1 的已有排班（排除自身 id），
     * 逐条判断时间段是否重叠。
     *
     * <p>重叠判定：{@code newStart < existEnd && newEnd > existStart}。
     *
     * @param excludeId 更新时排除自身 id；新增时传 null
     */
    private void checkOverlap(String butlerCode, LocalDate scheduleDate,
                              LocalTime newStart, LocalTime newEnd, Long excludeId) {
        List<ButlerSchedule> existing = butlerScheduleMapper.selectList(
                new LambdaQueryWrapper<ButlerSchedule>()
                        .eq(ButlerSchedule::getButlerCode, butlerCode)
                        .eq(ButlerSchedule::getScheduleDate, scheduleDate)
                        .eq(ButlerSchedule::getStatus, STATUS_ACTIVE));
        for (ButlerSchedule s : existing) {
            if (excludeId != null && excludeId.equals(s.getId())) {
                continue;
            }
            if (s.getStartTime() == null || s.getEndTime() == null) {
                continue;
            }
            // 区间重叠：newStart < existEnd && newEnd > existStart
            if (newStart.isBefore(s.getEndTime()) && newEnd.isAfter(s.getStartTime())) {
                throw new BusinessException(ErrorCode.BUSINESS,
                        "排班时间与已有排班冲突: " + scheduleDate + " "
                                + s.getStartTime() + "-" + s.getEndTime());
            }
        }
    }

    private LambdaQueryWrapper<ButlerSchedule> buildQueryWrapper(ButlerScheduleQueryDTO query) {
        return new LambdaQueryWrapper<ButlerSchedule>()
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerSchedule::getButlerCode, query.getButlerCode())
                .eq(query.getScheduleDate() != null,
                        ButlerSchedule::getScheduleDate, query.getScheduleDate())
                .ge(query.getScheduleDateStart() != null,
                        ButlerSchedule::getScheduleDate, query.getScheduleDateStart())
                .le(query.getScheduleDateEnd() != null,
                        ButlerSchedule::getScheduleDate, query.getScheduleDateEnd())
                .eq(query.getScheduleType() != null,
                        ButlerSchedule::getScheduleType, query.getScheduleType())
                .eq(query.getStatus() != null,
                        ButlerSchedule::getStatus, query.getStatus())
                .orderByAsc(ButlerSchedule::getScheduleDate)
                .orderByAsc(ButlerSchedule::getStartTime);
    }

    private ButlerSchedule requireSchedule(Long id) {
        ButlerSchedule schedule = butlerScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家排班不存在: " + id);
        }
        return schedule;
    }

    private ButlerScheduleVO toVO(ButlerSchedule entity) {
        ButlerScheduleVO vo = new ButlerScheduleVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setScheduleDate(entity.getScheduleDate());
        vo.setScheduleType(entity.getScheduleType());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
