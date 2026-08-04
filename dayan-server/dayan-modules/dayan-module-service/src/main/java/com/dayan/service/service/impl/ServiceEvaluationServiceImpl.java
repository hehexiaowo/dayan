package com.dayan.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceEvaluationCreateDTO;
import com.dayan.service.dto.ServiceEvaluationQueryDTO;
import com.dayan.service.dto.ServiceEvaluationUpdateDTO;
import com.dayan.service.entity.ServiceEvaluation;
import com.dayan.service.mapper.ServiceEvaluationMapper;
import com.dayan.service.service.ServiceEvaluationService;
import com.dayan.service.vo.ServiceEvaluationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务评价（service_evaluation）服务实现。
 *
 * <p><b>一会话一评价</b>：create 时按 sessionCode 查询，若已存在评价则抛
 * {@code BusinessException("该服务会话已存在评价")}。4 维评分（attitude/professional/
 * responsiveness/satisfaction，1-5）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceEvaluationServiceImpl implements ServiceEvaluationService {

    /** 评价默认状态：1=有效 */
    private static final int STATUS_NORMAL = 1;

    private final ServiceEvaluationMapper evaluationMapper;

    @Override
    public PageResult<ServiceEvaluationVO> page(ServiceEvaluationQueryDTO query) {
        LambdaQueryWrapper<ServiceEvaluation> wrapper = buildWrapper(query);
        Page<ServiceEvaluation> page = evaluationMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ServiceEvaluationVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ServiceEvaluationVO> list(ServiceEvaluationQueryDTO query) {
        return evaluationMapper.selectList(buildWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ServiceEvaluationVO getDetail(Long id) {
        return toVO(requireEvaluation(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ServiceEvaluationCreateDTO dto) {
        // 一会话一评价校验
        Long existCount = evaluationMapper.selectCount(new LambdaQueryWrapper<ServiceEvaluation>()
                .eq(ServiceEvaluation::getSessionCode, dto.getSessionCode()));
        if (existCount != null && existCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "该服务会话已存在评价");
        }

        ServiceEvaluation entity = new ServiceEvaluation();
        entity.setSessionCode(dto.getSessionCode());
        entity.setClientCode(dto.getClientCode());
        entity.setButlerCode(dto.getButlerCode());
        entity.setParkCode(dto.getParkCode());
        entity.setAttitudeRating(dto.getAttitudeRating());
        entity.setProfessionalRating(dto.getProfessionalRating());
        entity.setResponsivenessRating(dto.getResponsivenessRating());
        entity.setSatisfactionRating(dto.getSatisfactionRating());
        entity.setContent(dto.getContent());
        entity.setImageUrls(dto.getImageUrls());
        entity.setIsAnonymous(dto.getIsAnonymous() == null ? 0 : dto.getIsAnonymous());
        entity.setStatus(STATUS_NORMAL);
        evaluationMapper.insert(entity);
        log.info("创建服务评价成功: sessionCode={}, id={}", dto.getSessionCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ServiceEvaluationUpdateDTO dto) {
        ServiceEvaluation existing = requireEvaluation(id);

        ServiceEvaluation update = new ServiceEvaluation();
        update.setId(existing.getId());
        if (dto.getAttitudeRating() != null) update.setAttitudeRating(dto.getAttitudeRating());
        if (dto.getProfessionalRating() != null) update.setProfessionalRating(dto.getProfessionalRating());
        if (dto.getResponsivenessRating() != null) update.setResponsivenessRating(dto.getResponsivenessRating());
        if (dto.getSatisfactionRating() != null) update.setSatisfactionRating(dto.getSatisfactionRating());
        if (dto.getContent() != null) update.setContent(dto.getContent());
        if (dto.getImageUrls() != null) update.setImageUrls(dto.getImageUrls());
        if (dto.getIsAnonymous() != null) update.setIsAnonymous(dto.getIsAnonymous());
        // 回复字段：传入 replyContent 时同步写 replyTime（未显式提供则取当前时间）
        if (dto.getReplyContent() != null) {
            update.setReplyContent(dto.getReplyContent());
            update.setReplyTime(dto.getReplyTime() == null ? LocalDateTime.now() : dto.getReplyTime());
            if (dto.getReplyByCode() != null) {
                update.setReplyByCode(dto.getReplyByCode());
            }
        }
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        evaluationMapper.updateById(update);
        log.info("更新服务评价成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ServiceEvaluation existing = requireEvaluation(id);
        evaluationMapper.deleteById(existing.getId());
        log.info("删除服务评价成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ServiceEvaluation> buildWrapper(ServiceEvaluationQueryDTO query) {
        LambdaQueryWrapper<ServiceEvaluation> wrapper = new LambdaQueryWrapper<ServiceEvaluation>()
                .orderByDesc(ServiceEvaluation::getId);
        if (query.getSessionCode() != null && !query.getSessionCode().isEmpty()) {
            wrapper.eq(ServiceEvaluation::getSessionCode, query.getSessionCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ServiceEvaluation::getClientCode, query.getClientCode());
        }
        if (query.getButlerCode() != null && !query.getButlerCode().isEmpty()) {
            wrapper.eq(ServiceEvaluation::getButlerCode, query.getButlerCode());
        }
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ServiceEvaluation::getParkCode, query.getParkCode());
        }
        if (query.getIsAnonymous() != null) {
            wrapper.eq(ServiceEvaluation::getIsAnonymous, query.getIsAnonymous());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ServiceEvaluation::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ServiceEvaluation requireEvaluation(Long id) {
        ServiceEvaluation entity = evaluationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "服务评价不存在: id=" + id);
        }
        return entity;
    }

    private ServiceEvaluationVO toVO(ServiceEvaluation entity) {
        ServiceEvaluationVO vo = new ServiceEvaluationVO();
        vo.setId(entity.getId());
        vo.setSessionCode(entity.getSessionCode());
        vo.setClientCode(entity.getClientCode());
        vo.setButlerCode(entity.getButlerCode());
        vo.setParkCode(entity.getParkCode());
        vo.setAttitudeRating(entity.getAttitudeRating());
        vo.setProfessionalRating(entity.getProfessionalRating());
        vo.setResponsivenessRating(entity.getResponsivenessRating());
        vo.setSatisfactionRating(entity.getSatisfactionRating());
        vo.setContent(entity.getContent());
        vo.setImageUrls(entity.getImageUrls());
        vo.setIsAnonymous(entity.getIsAnonymous());
        vo.setReplyContent(entity.getReplyContent());
        vo.setReplyTime(entity.getReplyTime());
        vo.setReplyByCode(entity.getReplyByCode());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
