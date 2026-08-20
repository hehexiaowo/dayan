package com.dayan.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.course.dto.CourseRecordLearnCreateDTO;
import com.dayan.course.dto.CourseRecordLearnQueryDTO;
import com.dayan.course.dto.CourseRecordLearnUpdateDTO;
import com.dayan.course.entity.CourseInfo;
import com.dayan.course.entity.CourseRecordLearn;
import com.dayan.course.mapper.CourseInfoMapper;
import com.dayan.course.mapper.CourseRecordLearnMapper;
import com.dayan.course.service.CourseRecordLearnService;
import com.dayan.course.vo.CourseRecordLearnVO;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习记录服务实现（分片表，雪花ID）。
 *
 * <p>进度维护：
 * <ul>
 *   <li>create：currentLesson 默认 0，learnProgress 按 currentLesson/totalLesson 计算</li>
 *   <li>update：currentLesson/totalLearnTime 变更时刷新 learnProgress 与 lastLearnTime</li>
 *   <li>isCompleted=1 时自动补 completeTime</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRecordLearnServiceImpl implements CourseRecordLearnService {

    /** 状态：学习中 */
    private static final int STATUS_LEARNING = 1;
    /** 是否完成：否 */
    private static final int NOT_COMPLETED = 0;
    /** 是否完成：是 */
    private static final int COMPLETED = 1;

    private final CourseRecordLearnMapper courseRecordLearnMapper;
    private final CourseInfoMapper courseInfoMapper;

    @Override
    public PageResult<CourseRecordLearnVO> page(CourseRecordLearnQueryDTO query) {
        LambdaQueryWrapper<CourseRecordLearn> wrapper = buildQueryWrapper(query);
        Page<CourseRecordLearn> page = courseRecordLearnMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<CourseRecordLearnVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<CourseRecordLearnVO> list(CourseRecordLearnQueryDTO query) {
        LambdaQueryWrapper<CourseRecordLearn> wrapper = buildQueryWrapper(query);
        return courseRecordLearnMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public CourseRecordLearnVO getDetail(Long id) {
        return toVO(requireRecord(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CourseRecordLearnCreateDTO dto) {
        CourseRecordLearn entity = new CourseRecordLearn();
        entity.setCourseCode(dto.getCourseCode());
        entity.setClientCode(dto.getClientCode());
        entity.setAgentCode(dto.getAgentCode());
        entity.setLearnerName(dto.getLearnerName());
        entity.setLearnerPhone(dto.getLearnerPhone());
        entity.setEnrollTime(dto.getEnrollTime() == null ? LocalDateTime.now() : dto.getEnrollTime());
        entity.setOrderCode(dto.getOrderCode());

        int totalLesson = dto.getTotalLesson() == null ? 0 : dto.getTotalLesson();
        int currentLesson = dto.getCurrentLesson() == null ? 0 : dto.getCurrentLesson();
        entity.setTotalLesson(totalLesson);
        entity.setCurrentLesson(currentLesson);
        entity.setLearnProgress(calcProgress(currentLesson, totalLesson));

        entity.setTotalLearnTime(dto.getTotalLearnTime() == null ? 0 : dto.getTotalLearnTime());
        entity.setLastLearnTime(LocalDateTime.now());
        entity.setIsCompleted(NOT_COMPLETED);
        entity.setStatus(dto.getStatus() == null ? STATUS_LEARNING : dto.getStatus());

        courseRecordLearnMapper.insert(entity);
        log.info("创建学习记录成功: id={}, courseCode={}, learnerName={}",
                entity.getId(), dto.getCourseCode(), dto.getLearnerName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CourseRecordLearnUpdateDTO dto) {
        CourseRecordLearn existing = requireRecord(id);
        CourseRecordLearn update = new CourseRecordLearn();
        update.setId(existing.getId());

        // 进度相关字段合并计算
        Integer currentLesson = dto.getCurrentLesson() != null ? dto.getCurrentLesson() : existing.getCurrentLesson();
        Integer totalLesson = dto.getTotalLesson() != null ? dto.getTotalLesson() : existing.getTotalLesson();
        Integer totalLearnTime = dto.getTotalLearnTime() != null ? dto.getTotalLearnTime() : existing.getTotalLearnTime();
        boolean progressTouched = dto.getCurrentLesson() != null
                || dto.getTotalLesson() != null
                || dto.getTotalLearnTime() != null;

        if (dto.getLearnerName() != null) update.setLearnerName(dto.getLearnerName());
        if (dto.getLearnerPhone() != null) update.setLearnerPhone(dto.getLearnerPhone());
        if (dto.getOrderCode() != null) update.setOrderCode(dto.getOrderCode());
        if (dto.getCurrentLesson() != null) update.setCurrentLesson(currentLesson);
        if (dto.getTotalLesson() != null) update.setTotalLesson(totalLesson);
        if (dto.getTotalLearnTime() != null) update.setTotalLearnTime(totalLearnTime);
        if (dto.getLearnProgress() != null) {
            update.setLearnProgress(dto.getLearnProgress());
        } else if (progressTouched) {
            // 自动按 currentLesson/totalLesson 推算
            update.setLearnProgress(calcProgress(safeInt(currentLesson), safeInt(totalLesson)));
        }
        if (dto.getLastLearnTime() != null) {
            update.setLastLearnTime(dto.getLastLearnTime());
        } else if (progressTouched) {
            update.setLastLearnTime(LocalDateTime.now());
        }

        // 完成态
        Integer newIsCompleted = dto.getIsCompleted() != null ? dto.getIsCompleted() : existing.getIsCompleted();
        if (dto.getIsCompleted() != null) update.setIsCompleted(dto.getIsCompleted());
        if (dto.getCompleteTime() != null) {
            update.setCompleteTime(dto.getCompleteTime());
        } else if (newIsCompleted != null && newIsCompleted == COMPLETED && existing.getCompleteTime() == null) {
            // 标记完成时自动补完成时间
            update.setCompleteTime(LocalDateTime.now());
        }
        if (dto.getCertificateUrl() != null) update.setCertificateUrl(dto.getCertificateUrl());
        if (dto.getRating() != null) update.setRating(dto.getRating());
        if (dto.getRatingContent() != null) update.setRatingContent(dto.getRatingContent());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        courseRecordLearnMapper.updateById(update);
        log.info("更新学习记录成功: id={}, courseCode={}", id, existing.getCourseCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CourseRecordLearn existing = requireRecord(id);
        courseRecordLearnMapper.deleteById(existing.getId());
        log.info("删除学习记录成功: id={}, courseCode={}", id, existing.getCourseCode());
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<CourseRecordLearn> buildQueryWrapper(CourseRecordLearnQueryDTO query) {
        return new LambdaQueryWrapper<CourseRecordLearn>()
                .eq(query.getCourseCode() != null && !query.getCourseCode().isEmpty(),
                        CourseRecordLearn::getCourseCode, query.getCourseCode())
                .eq(query.getClientCode() != null && !query.getClientCode().isEmpty(),
                        CourseRecordLearn::getClientCode, query.getClientCode())
                .eq(query.getAgentCode() != null && !query.getAgentCode().isEmpty(),
                        CourseRecordLearn::getAgentCode, query.getAgentCode())
                .like(query.getLearnerName() != null && !query.getLearnerName().isEmpty(),
                        CourseRecordLearn::getLearnerName, query.getLearnerName())
                .eq(query.getLearnerPhone() != null && !query.getLearnerPhone().isEmpty(),
                        CourseRecordLearn::getLearnerPhone, query.getLearnerPhone())
                .eq(query.getIsCompleted() != null, CourseRecordLearn::getIsCompleted, query.getIsCompleted())
                .eq(query.getStatus() != null, CourseRecordLearn::getStatus, query.getStatus())
                .orderByDesc(CourseRecordLearn::getLastLearnTime)
                .orderByDesc(CourseRecordLearn::getCreatedAt);
    }

    CourseRecordLearn requireRecord(Long id) {
        CourseRecordLearn record = courseRecordLearnMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习记录不存在: id=" + id);
        }
        return record;
    }

    /** 计算进度百分比：currentLesson/totalLesson * 100，保留 2 位小数 */
    private BigDecimal calcProgress(int currentLesson, int totalLesson) {
        if (totalLesson <= 0) {
            return BigDecimal.ZERO;
        }
        int current = Math.max(0, currentLesson);
        if (current >= totalLesson) {
            return new BigDecimal("100.00");
        }
        return BigDecimal.valueOf(current)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalLesson), 2, RoundingMode.HALF_UP);
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private CourseRecordLearnVO toVO(CourseRecordLearn entity) {
        CourseRecordLearnVO vo = new CourseRecordLearnVO();
        vo.setId(entity.getId());
        vo.setCourseCode(entity.getCourseCode());
        vo.setClientCode(entity.getClientCode());
        vo.setAgentCode(entity.getAgentCode());
        vo.setLearnerName(entity.getLearnerName());
        vo.setLearnerPhone(entity.getLearnerPhone());
        vo.setEnrollTime(entity.getEnrollTime());
        vo.setOrderCode(entity.getOrderCode());
        vo.setCurrentLesson(entity.getCurrentLesson());
        vo.setTotalLesson(entity.getTotalLesson());
        vo.setLearnProgress(entity.getLearnProgress());
        vo.setTotalLearnTime(entity.getTotalLearnTime());
        vo.setLastLearnTime(entity.getLastLearnTime());
        vo.setIsCompleted(entity.getIsCompleted());
        vo.setCompleteTime(entity.getCompleteTime());
        vo.setCertificateUrl(entity.getCertificateUrl());
        vo.setRating(entity.getRating());
        vo.setRatingContent(entity.getRatingContent());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    // ====== Agent 端 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseRecordLearnVO reportProgress(String agentCode, String courseCode,
                                              Integer currentLesson, Integer learnTimeDelta) {
        // 查已有记录
        CourseRecordLearn record = courseRecordLearnMapper.selectOne(
                new LambdaQueryWrapper<CourseRecordLearn>()
                        .eq(CourseRecordLearn::getAgentCode, agentCode)
                        .eq(CourseRecordLearn::getCourseCode, courseCode)
                        .last("LIMIT 1"));

        if (record == null) {
            // 首次学习，自动创建记录
            CourseInfo course = courseInfoMapper.selectOne(
                    new LambdaQueryWrapper<CourseInfo>()
                            .eq(CourseInfo::getCourseCode, courseCode)
                            .last("LIMIT 1"));
            if (course == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在: " + courseCode);
            }

            record = new CourseRecordLearn();
            record.setAgentCode(agentCode);
            record.setCourseCode(courseCode);
            // learner_name 非空约束：优先取登录账号姓名，兜底 agentCode
            String accountName = ContextHolder.getAccountName();
            record.setLearnerName(accountName == null || accountName.isEmpty()
                    ? agentCode : accountName);
            record.setTotalLesson(course.getTotalClass() == null ? 0 : course.getTotalClass());
            record.setCurrentLesson(currentLesson == null ? 0 : currentLesson);
            record.setLearnProgress(calcProgress(
                    currentLesson == null ? 0 : currentLesson,
                    record.getTotalLesson()));
            record.setTotalLearnTime(learnTimeDelta == null ? 0 : Math.max(0, learnTimeDelta));
            record.setLastLearnTime(LocalDateTime.now());
            record.setEnrollTime(LocalDateTime.now());
            record.setIsCompleted(NOT_COMPLETED);
            record.setStatus(STATUS_LEARNING);
            courseRecordLearnMapper.insert(record);
            log.info("Agent 首次学习自动创建记录: agentCode={}, courseCode={}", agentCode, courseCode);
        } else {
            // 更新已有记录
            CourseRecordLearn update = new CourseRecordLearn();
            update.setId(record.getId());

            if (currentLesson != null) {
                update.setCurrentLesson(currentLesson);
                int totalLesson = record.getTotalLesson() == null ? 0 : record.getTotalLesson();
                update.setLearnProgress(calcProgress(currentLesson, totalLesson));
            }
            if (learnTimeDelta != null && learnTimeDelta > 0) {
                int prev = record.getTotalLearnTime() == null ? 0 : record.getTotalLearnTime();
                update.setTotalLearnTime(prev + learnTimeDelta);
            }
            update.setLastLearnTime(LocalDateTime.now());

            // 自动判定完成
            int newLesson = currentLesson != null ? currentLesson
                    : (record.getCurrentLesson() == null ? 0 : record.getCurrentLesson());
            int totalLesson = record.getTotalLesson() == null ? 0 : record.getTotalLesson();
            if (totalLesson > 0 && newLesson >= totalLesson
                    && (record.getIsCompleted() == null || record.getIsCompleted() == NOT_COMPLETED)) {
                update.setIsCompleted(COMPLETED);
                update.setCompleteTime(LocalDateTime.now());
                update.setStatus(2); // 已完成
                log.info("Agent 学习完成自动标记: agentCode={}, courseCode={}", agentCode, courseCode);
            }

            courseRecordLearnMapper.updateById(update);
            // 刷新内存对象用于返回
            if (currentLesson != null) record.setCurrentLesson(currentLesson);
            if (update.getTotalLearnTime() != null) record.setTotalLearnTime(update.getTotalLearnTime());
            if (update.getLearnProgress() != null) record.setLearnProgress(update.getLearnProgress());
            if (update.getLastLearnTime() != null) record.setLastLearnTime(update.getLastLearnTime());
            if (update.getIsCompleted() != null) record.setIsCompleted(update.getIsCompleted());
            if (update.getCompleteTime() != null) record.setCompleteTime(update.getCompleteTime());
            if (update.getStatus() != null) record.setStatus(update.getStatus());
        }
        return toVO(record);
    }

    @Override
    public CourseRecordLearnVO getMyRecord(String agentCode, String courseCode) {
        CourseRecordLearn record = courseRecordLearnMapper.selectOne(
                new LambdaQueryWrapper<CourseRecordLearn>()
                        .eq(CourseRecordLearn::getAgentCode, agentCode)
                        .eq(CourseRecordLearn::getCourseCode, courseCode)
                        .last("LIMIT 1"));
        return record == null ? null : toVO(record);
    }

    @Override
    public List<CourseRecordLearnVO> listMyRecords(String agentCode) {
        return courseRecordLearnMapper.selectList(
                new LambdaQueryWrapper<CourseRecordLearn>()
                        .eq(CourseRecordLearn::getAgentCode, agentCode)
                        .orderByDesc(CourseRecordLearn::getLastLearnTime))
                .stream().map(this::toVO).toList();
    }
}
