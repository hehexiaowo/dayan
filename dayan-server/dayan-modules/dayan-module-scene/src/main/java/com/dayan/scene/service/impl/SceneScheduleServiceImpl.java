package com.dayan.scene.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneScheduleCreateDTO;
import com.dayan.scene.dto.SceneScheduleQueryDTO;
import com.dayan.scene.dto.SceneScheduleUpdateDTO;
import com.dayan.scene.entity.SceneSchedule;
import com.dayan.scene.mapper.SceneScheduleMapper;
import com.dayan.scene.service.SceneScheduleService;
import com.dayan.scene.vo.SceneScheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/**
 * 场景日程服务实现。
 *
 * <p>{@code scene_schedule} 按 {@code sceneCode} 维度管理日程。
 *
 * <p><b>排期容量校验</b>：create/update 时校验 {@code currentPerson ≤ maxPerson}，
 * 超出抛 BusinessException("已报名人数不能超过最大参与人数")。
 * 若提供 {@code startTime}/{@code endTime}，则校验 {@code startTime < endTime}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneScheduleServiceImpl implements SceneScheduleService {

    /** 默认状态：开放 */
    private static final int DEFAULT_STATUS = 1;
    /** 默认已报名人数 */
    private static final int DEFAULT_CURRENT_PERSON = 0;

    private final SceneScheduleMapper sceneScheduleMapper;

    @Override
    public PageResult<SceneScheduleVO> page(SceneScheduleQueryDTO query) {
        LambdaQueryWrapper<SceneSchedule> wrapper = buildQueryWrapper(query);
        Page<SceneSchedule> page = sceneScheduleMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SceneScheduleVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<SceneScheduleVO> list(SceneScheduleQueryDTO query) {
        LambdaQueryWrapper<SceneSchedule> wrapper = buildQueryWrapper(query);
        return sceneScheduleMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public SceneScheduleVO getDetail(Long id) {
        return toVO(requireSchedule(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SceneScheduleCreateDTO dto) {
        Integer maxPerson = dto.getMaxPerson();
        Integer currentPerson = dto.getCurrentPerson() == null ? DEFAULT_CURRENT_PERSON : dto.getCurrentPerson();
        validateCapacity(currentPerson, maxPerson);
        validateTimeRange(dto.getStartTime(), dto.getEndTime());

        SceneSchedule entity = new SceneSchedule();
        entity.setSceneCode(dto.getSceneCode());
        entity.setScheduleDate(dto.getScheduleDate());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setMaxPerson(maxPerson);
        entity.setCurrentPerson(currentPerson);
        entity.setPriceOverride(dto.getPriceOverride());
        entity.setRemark(dto.getRemark());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        sceneScheduleMapper.insert(entity);
        log.info("创建场景日程成功: id={}, sceneCode={}, date={}",
                entity.getId(), dto.getSceneCode(), dto.getScheduleDate());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SceneScheduleUpdateDTO dto) {
        SceneSchedule existing = requireSchedule(id);

        Integer newMax = dto.getMaxPerson() != null ? dto.getMaxPerson() : existing.getMaxPerson();
        Integer newCur = dto.getCurrentPerson() != null ? dto.getCurrentPerson() : existing.getCurrentPerson();
        validateCapacity(newCur, newMax);

        LocalTime newStart = dto.getStartTime() != null ? dto.getStartTime() : existing.getStartTime();
        LocalTime newEnd = dto.getEndTime() != null ? dto.getEndTime() : existing.getEndTime();
        validateTimeRange(newStart, newEnd);

        SceneSchedule update = new SceneSchedule();
        update.setId(existing.getId());
        if (dto.getScheduleDate() != null) update.setScheduleDate(dto.getScheduleDate());
        update.setStartTime(newStart);
        update.setEndTime(newEnd);
        update.setMaxPerson(newMax);
        update.setCurrentPerson(newCur);
        if (dto.getPriceOverride() != null) update.setPriceOverride(dto.getPriceOverride());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        sceneScheduleMapper.updateById(update);
        log.info("更新场景日程成功: id={}, sceneCode={}", id, existing.getSceneCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SceneSchedule existing = requireSchedule(id);
        sceneScheduleMapper.deleteById(existing.getId());
        log.info("删除场景日程成功: id={}, sceneCode={}", id, existing.getSceneCode());
    }

    // ====== 内部方法 ======

    /**
     * 容量校验：currentPerson ≤ maxPerson。
     *
     * @param currentPerson 已报名人数
     * @param maxPerson     最大参与人数
     */
    private void validateCapacity(Integer currentPerson, Integer maxPerson) {
        if (maxPerson == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最大参与人数不能为空");
        }
        if (maxPerson < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最大参与人数不能为负数");
        }
        if (currentPerson == null) {
            currentPerson = DEFAULT_CURRENT_PERSON;
        }
        if (currentPerson < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已报名人数不能为负数");
        }
        if (currentPerson > maxPerson) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "已报名人数不能超过最大参与人数: current=" + currentPerson + ", max=" + maxPerson);
        }
    }

    /** 校验时间区间：若同时提供，则 startTime < endTime */
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "开始时间必须早于结束时间: start=" + startTime + ", end=" + endTime);
        }
    }

    private LambdaQueryWrapper<SceneSchedule> buildQueryWrapper(SceneScheduleQueryDTO query) {
        return new LambdaQueryWrapper<SceneSchedule>()
                .eq(query.getSceneCode() != null && !query.getSceneCode().isEmpty(),
                        SceneSchedule::getSceneCode, query.getSceneCode())
                .eq(query.getScheduleDate() != null,
                        SceneSchedule::getScheduleDate, query.getScheduleDate())
                .ge(query.getScheduleDateStart() != null,
                        SceneSchedule::getScheduleDate, query.getScheduleDateStart())
                .le(query.getScheduleDateEnd() != null,
                        SceneSchedule::getScheduleDate, query.getScheduleDateEnd())
                .eq(query.getStatus() != null,
                        SceneSchedule::getStatus, query.getStatus())
                .orderByAsc(SceneSchedule::getScheduleDate)
                .orderByAsc(SceneSchedule::getStartTime);
    }

    private SceneSchedule requireSchedule(Long id) {
        SceneSchedule schedule = sceneScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景日程不存在: " + id);
        }
        return schedule;
    }

    private SceneScheduleVO toVO(SceneSchedule entity) {
        SceneScheduleVO vo = new SceneScheduleVO();
        vo.setId(entity.getId());
        vo.setSceneCode(entity.getSceneCode());
        vo.setScheduleDate(entity.getScheduleDate());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setMaxPerson(entity.getMaxPerson());
        vo.setCurrentPerson(entity.getCurrentPerson());
        vo.setPriceOverride(entity.getPriceOverride());
        vo.setRemark(entity.getRemark());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
