package com.dayan.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.course.dto.CourseInfoCreateDTO;
import com.dayan.course.dto.CourseInfoQueryDTO;
import com.dayan.course.dto.CourseInfoUpdateDTO;
import com.dayan.course.entity.CourseInfo;
import com.dayan.course.mapper.CourseInfoMapper;
import com.dayan.course.service.CourseInfoService;
import com.dayan.course.vo.CourseInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 课程信息服务实现。
 *
 * <p>课程编码生成：{@code "CR" + String.format("%05d", sequenceProvider.next("code:seq:CR:0"))}，全表唯一。
 *
 * <p>容量约束：{@code currentStudents ≤ maxStudents}。create 时初始化 current=0，
 * update 时若下调 maxStudents 需校验不小于当前 currentStudents。状态：0=下架, 1=上架。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseInfoServiceImpl implements CourseInfoService {

    /** 课程编码前缀 */
    private static final String CODE_PREFIX = "CR";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:CR:0";
    /** 课程状态：下架 */
    private static final int STATUS_OFFLINE = 0;
    /** 课程状态：上架 */
    private static final int STATUS_ONLINE = 1;

    private final CourseInfoMapper courseInfoMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<CourseInfoVO> page(CourseInfoQueryDTO query) {
        LambdaQueryWrapper<CourseInfo> wrapper = buildQueryWrapper(query);
        Page<CourseInfo> page = courseInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<CourseInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<CourseInfoVO> list(CourseInfoQueryDTO query) {
        LambdaQueryWrapper<CourseInfo> wrapper = buildQueryWrapper(query);
        return courseInfoMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public CourseInfoVO getDetail(String courseCode) {
        return toVO(requireCourse(courseCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CourseInfoCreateDTO dto) {
        String courseCode = generateCourseCode();

        CourseInfo entity = new CourseInfo();
        entity.setCourseCode(courseCode);
        entity.setCourseName(dto.getCourseName());
        entity.setCourseType(dto.getCourseType());
        entity.setCategoryCode(dto.getCategoryCode());
        entity.setCoverImage(dto.getCoverImage());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setCourseDescription(dto.getCourseDescription());
        entity.setCourseOutline(dto.getCourseOutline());
        entity.setTargetAudience(dto.getTargetAudience());
        entity.setLearningObjectives(dto.getLearningObjectives());
        entity.setLecturerCode(dto.getLecturerCode());
        entity.setTotalClass(dto.getTotalClass());
        entity.setTotalDuration(dto.getTotalDuration());
        entity.setValidDays(dto.getValidDays());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setMaxStudents(dto.getMaxStudents());
        // 新建课程，当前学员数从 0 开始；满足 current ≤ max
        entity.setCurrentStudents(0);
        entity.setViewCount(0);
        entity.setSalesCount(0);
        entity.setIsFree(dto.getIsFree() == null ? 0 : dto.getIsFree());
        entity.setIsRecommend(dto.getIsRecommend() == null ? 0 : dto.getIsRecommend());
        entity.setCourseStartDate(dto.getCourseStartDate());
        entity.setCourseEndDate(dto.getCourseEndDate());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setCourseStatus(dto.getCourseStatus() == null ? STATUS_OFFLINE : dto.getCourseStatus());
        entity.setRemark(dto.getRemark());

        courseInfoMapper.insert(entity);
        log.info("创建课程成功: courseCode={}, courseName={}", courseCode, dto.getCourseName());
        return courseCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String courseCode, CourseInfoUpdateDTO dto) {
        CourseInfo existing = requireCourse(courseCode);
        CourseInfo update = new CourseInfo();
        update.setId(existing.getId());

        if (dto.getCourseName() != null) update.setCourseName(dto.getCourseName());
        if (dto.getCourseType() != null) update.setCourseType(dto.getCourseType());
        if (dto.getCategoryCode() != null) update.setCategoryCode(dto.getCategoryCode());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getVideoUrl() != null) update.setVideoUrl(dto.getVideoUrl());
        if (dto.getCourseDescription() != null) update.setCourseDescription(dto.getCourseDescription());
        if (dto.getCourseOutline() != null) update.setCourseOutline(dto.getCourseOutline());
        if (dto.getTargetAudience() != null) update.setTargetAudience(dto.getTargetAudience());
        if (dto.getLearningObjectives() != null) update.setLearningObjectives(dto.getLearningObjectives());
        if (dto.getLecturerCode() != null) update.setLecturerCode(dto.getLecturerCode());
        if (dto.getTotalClass() != null) update.setTotalClass(dto.getTotalClass());
        if (dto.getTotalDuration() != null) update.setTotalDuration(dto.getTotalDuration());
        if (dto.getValidDays() != null) update.setValidDays(dto.getValidDays());
        if (dto.getOriginalPrice() != null) update.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getSalePrice() != null) update.setSalePrice(dto.getSalePrice());
        if (dto.getMaxStudents() != null) {
            // 容量约束：新 maxStudents 不得小于已存在 currentStudents
            validateCapacity(dto.getMaxStudents(), existing.getCurrentStudents());
            update.setMaxStudents(dto.getMaxStudents());
        }
        if (dto.getIsFree() != null) update.setIsFree(dto.getIsFree());
        if (dto.getIsRecommend() != null) update.setIsRecommend(dto.getIsRecommend());
        if (dto.getCourseStartDate() != null) update.setCourseStartDate(dto.getCourseStartDate());
        if (dto.getCourseEndDate() != null) update.setCourseEndDate(dto.getCourseEndDate());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getCourseStatus() != null) update.setCourseStatus(dto.getCourseStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        courseInfoMapper.updateById(update);
        log.info("更新课程成功: courseCode={}", courseCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String courseCode) {
        CourseInfo existing = requireCourse(courseCode);
        courseInfoMapper.deleteById(existing.getId());
        log.info("删除课程成功: courseCode={}", courseCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(String courseCode) {
        CourseInfo existing = requireCourse(courseCode);
        if (existing.getCourseStatus() != null && existing.getCourseStatus() == STATUS_ONLINE) {
            throw new BusinessException(ErrorCode.BUSINESS, "课程已处于上架状态");
        }
        CourseInfo update = new CourseInfo();
        update.setId(existing.getId());
        update.setCourseStatus(STATUS_ONLINE);
        courseInfoMapper.updateById(update);
        log.info("课程上架成功: courseCode={}", courseCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(String courseCode) {
        CourseInfo existing = requireCourse(courseCode);
        if (existing.getCourseStatus() != null && existing.getCourseStatus() == STATUS_OFFLINE) {
            throw new BusinessException(ErrorCode.BUSINESS, "课程已处于下架状态");
        }
        CourseInfo update = new CourseInfo();
        update.setId(existing.getId());
        update.setCourseStatus(STATUS_OFFLINE);
        courseInfoMapper.updateById(update);
        log.info("课程下架成功: courseCode={}", courseCode);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<CourseInfo> buildQueryWrapper(CourseInfoQueryDTO query) {
        return new LambdaQueryWrapper<CourseInfo>()
                .eq(query.getCourseCode() != null && !query.getCourseCode().isEmpty(),
                        CourseInfo::getCourseCode, query.getCourseCode())
                .like(query.getCourseName() != null && !query.getCourseName().isEmpty(),
                        CourseInfo::getCourseName, query.getCourseName())
                .eq(query.getCourseType() != null, CourseInfo::getCourseType, query.getCourseType())
                .eq(query.getCategoryCode() != null && !query.getCategoryCode().isEmpty(),
                        CourseInfo::getCategoryCode, query.getCategoryCode())
                .eq(query.getLecturerCode() != null && !query.getLecturerCode().isEmpty(),
                        CourseInfo::getLecturerCode, query.getLecturerCode())
                .eq(query.getCourseStatus() != null, CourseInfo::getCourseStatus, query.getCourseStatus())
                .eq(query.getIsRecommend() != null, CourseInfo::getIsRecommend, query.getIsRecommend())
                .orderByAsc(CourseInfo::getSortOrder)
                .orderByDesc(CourseInfo::getCreatedAt);
    }

    CourseInfo requireCourse(String courseCode) {
        CourseInfo course = courseInfoMapper.selectOne(new LambdaQueryWrapper<CourseInfo>()
                .eq(CourseInfo::getCourseCode, courseCode)
                .last("LIMIT 1"));
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在: " + courseCode);
        }
        return course;
    }

    /** 容量校验：当前学员数不得超过最大容量。 */
    private void validateCapacity(Integer maxStudents, Integer currentStudents) {
        if (maxStudents == null) {
            return; // null 表示不限
        }
        if (maxStudents < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最大学员数不能为负数");
        }
        int current = currentStudents == null ? 0 : currentStudents;
        if (current > maxStudents) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "最大学员数不能小于当前学员数（当前 " + current + " 人）");
        }
    }

    /** 生成课程编码：CR + 5 位序列 */
    private String generateCourseCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private CourseInfoVO toVO(CourseInfo entity) {
        CourseInfoVO vo = new CourseInfoVO();
        vo.setId(entity.getId());
        vo.setCourseCode(entity.getCourseCode());
        vo.setCourseName(entity.getCourseName());
        vo.setCourseType(entity.getCourseType());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCoverImage(entity.getCoverImage());
        vo.setVideoUrl(entity.getVideoUrl());
        vo.setCourseDescription(entity.getCourseDescription());
        vo.setCourseOutline(entity.getCourseOutline());
        vo.setTargetAudience(entity.getTargetAudience());
        vo.setLearningObjectives(entity.getLearningObjectives());
        vo.setLecturerCode(entity.getLecturerCode());
        vo.setTotalClass(entity.getTotalClass());
        vo.setTotalDuration(entity.getTotalDuration());
        vo.setValidDays(entity.getValidDays());
        vo.setOriginalPrice(entity.getOriginalPrice());
        vo.setSalePrice(entity.getSalePrice());
        vo.setMaxStudents(entity.getMaxStudents());
        vo.setCurrentStudents(entity.getCurrentStudents());
        vo.setViewCount(entity.getViewCount());
        vo.setSalesCount(entity.getSalesCount());
        vo.setRatingAvg(entity.getRatingAvg());
        vo.setIsFree(entity.getIsFree());
        vo.setIsRecommend(entity.getIsRecommend());
        vo.setCourseStartDate(entity.getCourseStartDate());
        vo.setCourseEndDate(entity.getCourseEndDate());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCourseStatus(entity.getCourseStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
