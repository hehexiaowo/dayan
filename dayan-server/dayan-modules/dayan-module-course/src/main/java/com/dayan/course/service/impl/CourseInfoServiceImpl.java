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
import com.dayan.course.entity.CourseLecturer;
import com.dayan.course.mapper.CourseInfoMapper;
import com.dayan.course.mapper.CourseLecturerMapper;
import com.dayan.course.service.CourseInfoService;
import com.dayan.course.vo.CourseAgentVO;
import com.dayan.course.vo.CourseInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程信息服务实现。
 *
 * <p>课程编码生成：{@code "CR" + String.format("%05d", sequenceProvider.next("code:seq:CR:0"))}，全表唯一。
 *
 * <p>容量约束：{@code currentStudents ≤ maxStudents}。create 时初始化 current=0，
 * update 时若下调 maxStudents 需校验不小于当前 currentStudents。状态（DDL 5 态）：0=草稿, 1=待上架, 2=已上架, 3=已下架, 4=已结课。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseInfoServiceImpl implements CourseInfoService {

    /** 课程编码前缀 */
    private static final String CODE_PREFIX = "CR";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:CR:0";
    /** 课程状态（DDL 5 态）：0=草稿, 1=待上架, 2=已上架, 3=已下架, 4=已结课 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_ONLINE = 2;
    private static final int STATUS_OFFLINE = 3;
    private static final int STATUS_FINISHED = 4;
    /** 板块来源：1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯 */
    private static final int COURSE_SOURCE_SELF = 1;

    private final CourseInfoMapper courseInfoMapper;
    private final CourseLecturerMapper courseLecturerMapper;
    private final SequenceProvider sequenceProvider;
    private final ChannelConfigCourseBridge channelConfigCourseBridge;

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
        entity.setCourseSource(dto.getCourseSource() == null ? COURSE_SOURCE_SELF : dto.getCourseSource());
        entity.setCategoryCode(dto.getCategoryCode());
        entity.setCoverImage(dto.getCoverImage());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setCourseDescription(dto.getCourseDescription());
        entity.setCourseBody(dto.getCourseBody());
        entity.setCourseOutline(dto.getCourseOutline());
        entity.setTargetAudience(dto.getTargetAudience());
        entity.setLearningObjectives(dto.getLearningObjectives());
        entity.setAuthor(dto.getAuthor());
        entity.setDurationText(dto.getDurationText());
        entity.setLecturerCode(dto.getLecturerCode());
        entity.setTotalClass(dto.getTotalClass() == null ? 0 : dto.getTotalClass());
        entity.setTotalDuration(dto.getTotalDuration());
        entity.setValidDays(dto.getValidDays());
        entity.setOriginalPrice(dto.getOriginalPrice() == null ? BigDecimal.ZERO : dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice() == null ? BigDecimal.ZERO : dto.getSalePrice());
        entity.setMaxStudents(dto.getMaxStudents());
        // 新建课程，当前学员数从 0 开始；满足 current ≤ max
        entity.setCurrentStudents(0);
        entity.setViewCount(0);
        entity.setSalesCount(0);
        entity.setIsFree(dto.getIsFree() == null ? 0 : dto.getIsFree());
        entity.setIsRecommend(dto.getIsRecommend() == null ? 0 : dto.getIsRecommend());
        entity.setCourseStartDate(dto.getCourseStartDate());
        entity.setCourseEndDate(dto.getCourseEndDate());
        entity.setBadge(dto.getBadge());
        entity.setPublishTime(dto.getPublishTime());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setCourseStatus(dto.getCourseStatus() == null ? STATUS_DRAFT : dto.getCourseStatus());
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
        if (dto.getCourseSource() != null) update.setCourseSource(dto.getCourseSource());
        if (dto.getCategoryCode() != null) update.setCategoryCode(dto.getCategoryCode());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getVideoUrl() != null) update.setVideoUrl(dto.getVideoUrl());
        if (dto.getCourseDescription() != null) update.setCourseDescription(dto.getCourseDescription());
        if (dto.getCourseBody() != null) update.setCourseBody(dto.getCourseBody());
        if (dto.getCourseOutline() != null) update.setCourseOutline(dto.getCourseOutline());
        if (dto.getTargetAudience() != null) update.setTargetAudience(dto.getTargetAudience());
        if (dto.getLearningObjectives() != null) update.setLearningObjectives(dto.getLearningObjectives());
        if (dto.getAuthor() != null) update.setAuthor(dto.getAuthor());
        if (dto.getDurationText() != null) update.setDurationText(dto.getDurationText());
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
        if (dto.getBadge() != null) update.setBadge(dto.getBadge());
        if (dto.getPublishTime() != null) update.setPublishTime(dto.getPublishTime());
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
        int status = existing.getCourseStatus() == null ? STATUS_DRAFT : existing.getCourseStatus();
        if (status == STATUS_ONLINE) {
            throw new BusinessException(ErrorCode.BUSINESS, "课程已处于上架状态");
        }
        if (status == STATUS_FINISHED) {
            throw new BusinessException(ErrorCode.BUSINESS, "已结课课程不可上架");
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
        int status = existing.getCourseStatus() == null ? STATUS_DRAFT : existing.getCourseStatus();
        if (status == STATUS_OFFLINE) {
            throw new BusinessException(ErrorCode.BUSINESS, "课程已处于下架状态");
        }
        if (status != STATUS_ONLINE) {
            throw new BusinessException(ErrorCode.BUSINESS, "仅上架课程可下架");
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
                .eq(query.getCourseSource() != null, CourseInfo::getCourseSource, query.getCourseSource())
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
        vo.setCourseSource(entity.getCourseSource());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCoverImage(entity.getCoverImage());
        vo.setVideoUrl(entity.getVideoUrl());
        vo.setCourseDescription(entity.getCourseDescription());
        vo.setCourseBody(entity.getCourseBody());
        vo.setCourseOutline(entity.getCourseOutline());
        vo.setTargetAudience(entity.getTargetAudience());
        vo.setLearningObjectives(entity.getLearningObjectives());
        vo.setAuthor(entity.getAuthor());
        vo.setDurationText(entity.getDurationText());
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
        vo.setBadge(entity.getBadge());
        vo.setPublishTime(entity.getPublishTime());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCourseStatus(entity.getCourseStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    // ====== Agent 端只读 ======

    @Override
    public List<CourseAgentVO> listPublished(Integer courseType, Integer courseSource) {
        LambdaQueryWrapper<CourseInfo> wrapper = new LambdaQueryWrapper<CourseInfo>()
                .eq(CourseInfo::getCourseStatus, STATUS_ONLINE)
                .eq(courseType != null, CourseInfo::getCourseType, courseType)
                .eq(courseSource != null, CourseInfo::getCourseSource, courseSource)
                .orderByDesc(CourseInfo::getSortOrder)
                .orderByDesc(CourseInfo::getCreatedAt);
        List<CourseInfo> courses = courseInfoMapper.selectList(wrapper);
        if (courses.isEmpty()) {
            return List.of();
        }
        // 批量取讲师名（一对一，量小）
        Map<String, String> nameMap = lecturerNameMap(courses.stream()
                .map(CourseInfo::getLecturerCode).filter(Objects::nonNull).toList());
        return courses.stream().map(c -> {
            CourseAgentVO vo = toAgentVO(c);
            vo.setLecturerName(c.getLecturerCode() == null ? null : nameMap.get(c.getLecturerCode()));
            return vo;
        }).toList();
    }

    @Override
    public PageResult<CourseAgentVO> pagePublished(String channelCode, Integer courseType, Integer courseSource,
                                                   String keyword, String categoryCode,
                                                   int current, int size) {
        // 渠道配置的课程
        List<String> configuredCodes = channelConfigCourseBridge.listConfiguredCourseCodes(channelCode);
        if (configuredCodes.isEmpty()) {
            return new PageResult<>(current, size, 0L, List.of());
        }

        LambdaQueryWrapper<CourseInfo> wrapper = new LambdaQueryWrapper<CourseInfo>()
                .in(CourseInfo::getCourseCode, configuredCodes)
                .eq(CourseInfo::getCourseStatus, STATUS_ONLINE)
                .eq(courseType != null, CourseInfo::getCourseType, courseType)
                .eq(courseSource != null, CourseInfo::getCourseSource, courseSource)
                .eq(categoryCode != null && !categoryCode.isEmpty(), CourseInfo::getCategoryCode, categoryCode)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(CourseInfo::getCourseName, keyword)
                        .or()
                        .like(CourseInfo::getCourseDescription, keyword))
                .orderByDesc(CourseInfo::getIsRecommend)
                .orderByAsc(CourseInfo::getSortOrder)
                .orderByDesc(CourseInfo::getCreatedAt);

        Page<CourseInfo> page = courseInfoMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(current, size, page.getTotal(), List.of());
        }
        Map<String, String> nameMap = lecturerNameMap(page.getRecords().stream()
                .map(CourseInfo::getLecturerCode).filter(Objects::nonNull).toList());
        List<CourseAgentVO> voList = page.getRecords().stream().map(c -> {
            CourseAgentVO vo = toAgentVO(c);
            vo.setLecturerName(c.getLecturerCode() == null ? null : nameMap.get(c.getLecturerCode()));
            return vo;
        }).toList();
        return new PageResult<>(current, size, page.getTotal(), voList);
    }

    @Override
    public Map<Integer, Long> countPublishedBySource(String channelCode) {
        // 渠道配置的课程
        List<String> configuredCodes = channelConfigCourseBridge.listConfiguredCourseCodes(channelCode);
        if (configuredCodes.isEmpty()) {
            return Map.of();
        }
        // 按 courseSource 分组计数，仅上架课程
        List<CourseInfo> courses = courseInfoMapper.selectList(
                new LambdaQueryWrapper<CourseInfo>()
                        .in(CourseInfo::getCourseCode, configuredCodes)
                        .eq(CourseInfo::getCourseStatus, STATUS_ONLINE)
                        .select(CourseInfo::getCourseSource));
        return courses.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCourseSource() == null ? COURSE_SOURCE_SELF : c.getCourseSource(),
                        Collectors.counting()));
    }

    @Override
    public List<CourseAgentVO> listAvailableForChannel(String channelCode) {
        // 平台课程（channel_code IS NULL）+ 本渠道课程（channel_code = channelCode）
        return courseInfoMapper.selectList(new LambdaQueryWrapper<CourseInfo>()
                        .eq(CourseInfo::getCourseStatus, STATUS_ONLINE)
                        .and(w -> w.isNull(CourseInfo::getChannelCode)
                                .or().eq(CourseInfo::getChannelCode, channelCode))
                        .orderByAsc(CourseInfo::getSortOrder))
                .stream().map(this::toAgentVO).toList();
    }

    @Override
    public List<CourseAgentVO> listForAgent(String channelCode, Integer courseType, Integer courseSource) {
        // 渠道配置的课程
        List<String> configuredCodes = channelConfigCourseBridge.listConfiguredCourseCodes(channelCode);
        if (configuredCodes.isEmpty()) {
            return List.of();
        }
        // 返回渠道配置的课程
        return courseInfoMapper.selectList(new LambdaQueryWrapper<CourseInfo>()
                        .in(CourseInfo::getCourseCode, configuredCodes)
                        .eq(CourseInfo::getCourseStatus, STATUS_ONLINE)
                        .eq(courseType != null, CourseInfo::getCourseType, courseType)
                        .eq(courseSource != null, CourseInfo::getCourseSource, courseSource)
                        .orderByAsc(CourseInfo::getSortOrder))
                .stream().map(this::toAgentVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseAgentVO getPublishedDetail(String courseCode) {
        CourseInfo course = requireCourse(courseCode);
        if (course.getCourseStatus() == null || course.getCourseStatus() != STATUS_ONLINE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在或未上架: " + courseCode);
        }
        // 浏览量 +1（最小字段更新，避免并发覆盖其他字段）
        CourseInfo bump = new CourseInfo();
        bump.setId(course.getId());
        bump.setViewCount((course.getViewCount() == null ? 0 : course.getViewCount()) + 1);
        courseInfoMapper.updateById(bump);
        course.setViewCount(bump.getViewCount());

        CourseAgentVO vo = toAgentVO(course);
        if (course.getLecturerCode() != null) {
            CourseLecturer l = courseLecturerMapper.selectOne(
                    new LambdaQueryWrapper<CourseLecturer>()
                            .eq(CourseLecturer::getLecturerCode, course.getLecturerCode())
                            .last("LIMIT 1"));
            if (l != null) {
                vo.setLecturerName(l.getLecturerName());
                CourseAgentVO.LecturerBrief brief = new CourseAgentVO.LecturerBrief();
                brief.setLecturerCode(l.getLecturerCode());
                brief.setLecturerName(l.getLecturerName());
                brief.setAvatar(l.getAvatar());
                brief.setTitle(l.getTitle());
                brief.setOrganization(l.getOrganization());
                brief.setIntroduction(l.getIntroduction());
                vo.setLecturer(brief);
            }
        }
        return vo;
    }

    /** CourseInfo → CourseAgentVO（不含讲师；调用方自行填充） */
    private CourseAgentVO toAgentVO(CourseInfo e) {
        CourseAgentVO vo = new CourseAgentVO();
        vo.setCourseCode(e.getCourseCode());
        vo.setCourseName(e.getCourseName());
        vo.setCourseType(e.getCourseType());
        vo.setCourseSource(e.getCourseSource());
        vo.setCategoryCode(e.getCategoryCode());
        vo.setCoverImage(e.getCoverImage());
        vo.setVideoUrl(e.getVideoUrl());
        vo.setCourseDescription(e.getCourseDescription());
        vo.setCourseBody(e.getCourseBody());
        vo.setCourseOutline(e.getCourseOutline());
        vo.setTargetAudience(e.getTargetAudience());
        vo.setLearningObjectives(e.getLearningObjectives());
        vo.setAuthor(e.getAuthor());
        vo.setDurationText(e.getDurationText());
        vo.setLecturerCode(e.getLecturerCode());
        vo.setTotalClass(e.getTotalClass());
        vo.setTotalDuration(e.getTotalDuration());
        vo.setValidDays(e.getValidDays());
        vo.setOriginalPrice(e.getOriginalPrice());
        vo.setSalePrice(e.getSalePrice());
        vo.setCurrentStudents(e.getCurrentStudents());
        vo.setMaxStudents(e.getMaxStudents());
        vo.setViewCount(e.getViewCount());
        vo.setSalesCount(e.getSalesCount());
        vo.setRatingAvg(e.getRatingAvg());
        vo.setIsFree(e.getIsFree());
        vo.setIsRecommend(e.getIsRecommend());
        vo.setCourseStartDate(e.getCourseStartDate());
        vo.setCourseEndDate(e.getCourseEndDate());
        vo.setBadge(e.getBadge());
        vo.setPublishTime(e.getPublishTime());
        vo.setSortOrder(e.getSortOrder());
        vo.setCourseStatus(e.getCourseStatus());
        return vo;
    }

    /** 批量查讲师编码→姓名 */
    private Map<String, String> lecturerNameMap(List<String> lecturerCodes) {
        if (lecturerCodes.isEmpty()) {
            return Map.of();
        }
        return courseLecturerMapper.selectList(new LambdaQueryWrapper<CourseLecturer>()
                        .in(CourseLecturer::getLecturerCode, lecturerCodes))
                .stream()
                .collect(Collectors.toMap(
                        CourseLecturer::getLecturerCode,
                        CourseLecturer::getLecturerName,
                        (a, b) -> a));
    }
}
