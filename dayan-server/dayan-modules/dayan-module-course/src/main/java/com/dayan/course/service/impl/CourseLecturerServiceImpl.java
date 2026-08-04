package com.dayan.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.course.dto.CourseLecturerCreateDTO;
import com.dayan.course.dto.CourseLecturerQueryDTO;
import com.dayan.course.dto.CourseLecturerUpdateDTO;
import com.dayan.course.entity.CourseLecturer;
import com.dayan.course.mapper.CourseLecturerMapper;
import com.dayan.course.service.CourseLecturerService;
import com.dayan.course.vo.CourseLecturerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 课程讲师服务实现。
 *
 * <p>讲师编码生成：{@code "LT" + String.format("%05d", sequenceProvider.next("code:seq:LT:0"))}，
 * 全表唯一，作为 course_info.lecturerCode 的关联键。新建默认 status=1（启用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseLecturerServiceImpl implements CourseLecturerService {

    /** 讲师编码前缀 */
    private static final String CODE_PREFIX = "LT";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:LT:0";
    /** 默认状态：启用 */
    private static final int DEFAULT_STATUS = 1;

    private final CourseLecturerMapper courseLecturerMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<CourseLecturerVO> page(CourseLecturerQueryDTO query) {
        LambdaQueryWrapper<CourseLecturer> wrapper = buildQueryWrapper(query);
        Page<CourseLecturer> page = courseLecturerMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<CourseLecturerVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<CourseLecturerVO> list(CourseLecturerQueryDTO query) {
        LambdaQueryWrapper<CourseLecturer> wrapper = buildQueryWrapper(query);
        return courseLecturerMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public CourseLecturerVO getDetail(Long id) {
        return toVO(requireLecturer(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CourseLecturerCreateDTO dto) {
        String lecturerCode = generateLecturerCode();

        CourseLecturer entity = new CourseLecturer();
        entity.setLecturerCode(lecturerCode);
        entity.setLecturerName(dto.getLecturerName());
        entity.setGender(dto.getGender());
        entity.setAvatar(dto.getAvatar());
        entity.setTitle(dto.getTitle());
        entity.setOrganization(dto.getOrganization());
        entity.setSpecialty(dto.getSpecialty());
        entity.setIntroduction(dto.getIntroduction());
        entity.setCertifications(dto.getCertifications());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setCourseCount(0);
        entity.setStudentCount(0);
        entity.setIsCertified(dto.getIsCertified() == null ? 0 : dto.getIsCertified());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        courseLecturerMapper.insert(entity);
        log.info("创建讲师成功: lecturerCode={}, lecturerName={}", lecturerCode, dto.getLecturerName());
        return lecturerCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CourseLecturerUpdateDTO dto) {
        CourseLecturer existing = requireLecturer(id);
        CourseLecturer update = new CourseLecturer();
        update.setId(existing.getId());

        if (dto.getLecturerName() != null) update.setLecturerName(dto.getLecturerName());
        if (dto.getGender() != null) update.setGender(dto.getGender());
        if (dto.getAvatar() != null) update.setAvatar(dto.getAvatar());
        if (dto.getTitle() != null) update.setTitle(dto.getTitle());
        if (dto.getOrganization() != null) update.setOrganization(dto.getOrganization());
        if (dto.getSpecialty() != null) update.setSpecialty(dto.getSpecialty());
        if (dto.getIntroduction() != null) update.setIntroduction(dto.getIntroduction());
        if (dto.getCertifications() != null) update.setCertifications(dto.getCertifications());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getEmail() != null) update.setEmail(dto.getEmail());
        if (dto.getIsCertified() != null) update.setIsCertified(dto.getIsCertified());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        courseLecturerMapper.updateById(update);
        log.info("更新讲师成功: id={}, lecturerCode={}", id, existing.getLecturerCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CourseLecturer existing = requireLecturer(id);
        courseLecturerMapper.deleteById(existing.getId());
        log.info("删除讲师成功: id={}, lecturerCode={}", id, existing.getLecturerCode());
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<CourseLecturer> buildQueryWrapper(CourseLecturerQueryDTO query) {
        return new LambdaQueryWrapper<CourseLecturer>()
                .eq(query.getLecturerCode() != null && !query.getLecturerCode().isEmpty(),
                        CourseLecturer::getLecturerCode, query.getLecturerCode())
                .like(query.getLecturerName() != null && !query.getLecturerName().isEmpty(),
                        CourseLecturer::getLecturerName, query.getLecturerName())
                .like(query.getOrganization() != null && !query.getOrganization().isEmpty(),
                        CourseLecturer::getOrganization, query.getOrganization())
                .eq(query.getIsCertified() != null, CourseLecturer::getIsCertified, query.getIsCertified())
                .eq(query.getStatus() != null, CourseLecturer::getStatus, query.getStatus())
                .orderByAsc(CourseLecturer::getSortOrder)
                .orderByDesc(CourseLecturer::getCreatedAt);
    }

    CourseLecturer requireLecturer(Long id) {
        CourseLecturer lecturer = courseLecturerMapper.selectById(id);
        if (lecturer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "讲师不存在: id=" + id);
        }
        return lecturer;
    }

    /** 生成讲师编码：LT + 5 位序列 */
    private String generateLecturerCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private CourseLecturerVO toVO(CourseLecturer entity) {
        CourseLecturerVO vo = new CourseLecturerVO();
        vo.setId(entity.getId());
        vo.setLecturerCode(entity.getLecturerCode());
        vo.setLecturerName(entity.getLecturerName());
        vo.setGender(entity.getGender());
        vo.setAvatar(entity.getAvatar());
        vo.setTitle(entity.getTitle());
        vo.setOrganization(entity.getOrganization());
        vo.setSpecialty(entity.getSpecialty());
        vo.setIntroduction(entity.getIntroduction());
        vo.setCertifications(entity.getCertifications());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setCourseCount(entity.getCourseCount());
        vo.setStudentCount(entity.getStudentCount());
        vo.setRatingAvg(entity.getRatingAvg());
        vo.setIsCertified(entity.getIsCertified());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
