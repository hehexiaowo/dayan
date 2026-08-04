package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.supplier.dto.SupplierEvaluationCreateDTO;
import com.dayan.supplier.dto.SupplierEvaluationQueryDTO;
import com.dayan.supplier.dto.SupplierEvaluationUpdateDTO;
import com.dayan.supplier.entity.SupplierEvaluation;
import com.dayan.supplier.mapper.SupplierEvaluationMapper;
import com.dayan.supplier.service.SupplierEvaluationService;
import com.dayan.supplier.vo.SupplierEvaluationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 供应商评估服务实现。
 *
 * <p>综合评分公式：
 * <pre>
 *   totalScore = (service + facility + cooperation) / 3 * (1 - complaintRate/100)   保留 2 位
 *   scoreLevel = 1(A>=90) / 2(B 80-89) / 3(C 70-79) / 4(D<70)
 * </pre>
 *
 * <p>计算规则：入参 {@code totalScore}/{@code scoreLevel} 为空时自动计算并覆盖；
 * 入参显式传入则以入参为准（规格 §3.4：自动计算覆盖默认场景）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierEvaluationServiceImpl implements SupplierEvaluationService {

    private final SupplierEvaluationMapper evaluationMapper;

    @Override
    public PageResult<SupplierEvaluationVO> page(SupplierEvaluationQueryDTO query) {
        LambdaQueryWrapper<SupplierEvaluation> wrapper = new LambdaQueryWrapper<SupplierEvaluation>()
                .eq(query.getSupplierCode() != null && !query.getSupplierCode().isEmpty(),
                        SupplierEvaluation::getSupplierCode, query.getSupplierCode())
                .eq(query.getEvalPeriod() != null && !query.getEvalPeriod().isEmpty(),
                        SupplierEvaluation::getEvalPeriod, query.getEvalPeriod())
                .eq(query.getEvalType() != null, SupplierEvaluation::getEvalType, query.getEvalType())
                .eq(query.getScoreLevel() != null, SupplierEvaluation::getScoreLevel, query.getScoreLevel())
                .eq(query.getStatus() != null, SupplierEvaluation::getStatus, query.getStatus())
                .orderByDesc(SupplierEvaluation::getCreatedAt);
        Page<SupplierEvaluation> page = evaluationMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SupplierEvaluationVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public SupplierEvaluationVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SupplierEvaluationCreateDTO dto) {
        SupplierEvaluation entity = new SupplierEvaluation();
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setEvalPeriod(dto.getEvalPeriod());
        entity.setEvalType(dto.getEvalType());
        entity.setServiceQualityScore(dto.getServiceQualityScore());
        entity.setFacilityQualityScore(dto.getFacilityQualityScore());
        entity.setCooperationScore(dto.getCooperationScore());
        entity.setComplaintRate(dto.getComplaintRate());
        entity.setTotalOrderCount(dto.getTotalOrderCount());
        entity.setComplaintCount(dto.getComplaintCount());
        entity.setEvalContent(dto.getEvalContent());
        entity.setImprovementSuggestions(dto.getImprovementSuggestions());
        entity.setEvaluatorCode(dto.getEvaluatorCode());
        entity.setEvaluatorName(dto.getEvaluatorName());
        entity.setEvalDate(dto.getEvalDate());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());

        // totalScore / scoreLevel 自动计算（入参未显式传时覆盖）
        applyScore(entity, dto.getTotalScore(), dto.getScoreLevel());

        evaluationMapper.insert(entity);
        log.info("创建供应商评估成功: id={}, supplierCode={}, evalPeriod={}, totalScore={}, scoreLevel={}",
                entity.getId(), entity.getSupplierCode(), entity.getEvalPeriod(),
                entity.getTotalScore(), entity.getScoreLevel());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SupplierEvaluationUpdateDTO dto) {
        SupplierEvaluation existing = requireById(id);
        SupplierEvaluation update = new SupplierEvaluation();
        update.setId(existing.getId());

        // 先拷贝可变字段，再在 update 上应用评分计算（基于 update + existing 合并值）
        if (dto.getEvalPeriod() != null) update.setEvalPeriod(dto.getEvalPeriod());
        if (dto.getEvalType() != null) update.setEvalType(dto.getEvalType());
        if (dto.getServiceQualityScore() != null) update.setServiceQualityScore(dto.getServiceQualityScore());
        if (dto.getFacilityQualityScore() != null) update.setFacilityQualityScore(dto.getFacilityQualityScore());
        if (dto.getCooperationScore() != null) update.setCooperationScore(dto.getCooperationScore());
        if (dto.getComplaintRate() != null) update.setComplaintRate(dto.getComplaintRate());
        if (dto.getTotalOrderCount() != null) update.setTotalOrderCount(dto.getTotalOrderCount());
        if (dto.getComplaintCount() != null) update.setComplaintCount(dto.getComplaintCount());
        if (dto.getEvalContent() != null) update.setEvalContent(dto.getEvalContent());
        if (dto.getImprovementSuggestions() != null) update.setImprovementSuggestions(dto.getImprovementSuggestions());
        if (dto.getEvaluatorCode() != null) update.setEvaluatorCode(dto.getEvaluatorCode());
        if (dto.getEvaluatorName() != null) update.setEvaluatorName(dto.getEvaluatorName());
        if (dto.getEvalDate() != null) update.setEvalDate(dto.getEvalDate());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        // 评分计算：基于合并后的最终值
        BigDecimal service = pick(dto.getServiceQualityScore(), existing.getServiceQualityScore());
        BigDecimal facility = pick(dto.getFacilityQualityScore(), existing.getFacilityQualityScore());
        BigDecimal cooperation = pick(dto.getCooperationScore(), existing.getCooperationScore());
        BigDecimal complaintRate = pick(dto.getComplaintRate(), existing.getComplaintRate());

        // 若入参未显式传 totalScore/scoreLevel，按公式自动算并写入 update
        if (dto.getTotalScore() == null) {
            BigDecimal total = calcTotalScore(service, facility, cooperation, complaintRate);
            if (total != null) {
                update.setTotalScore(total);
            }
        } else {
            update.setTotalScore(dto.getTotalScore());
        }
        if (dto.getScoreLevel() == null) {
            BigDecimal base = pick(dto.getTotalScore(), pick(existing.getTotalScore(), calcTotalScore(service, facility, cooperation, complaintRate)));
            Integer level = calcScoreLevel(base);
            if (level != null) {
                update.setScoreLevel(level);
            }
        } else {
            update.setScoreLevel(dto.getScoreLevel());
        }

        evaluationMapper.updateById(update);
        log.info("更新供应商评估成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        evaluationMapper.deleteById(id);
        log.info("删除供应商评估成功: id={}", id);
    }

    // ====== 内部方法 ======

    private SupplierEvaluation requireById(Long id) {
        SupplierEvaluation entity = evaluationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商评估不存在: id=" + id);
        }
        return entity;
    }

    /** 应用 totalScore / scoreLevel（入参未显式传时自动计算） */
    private void applyScore(SupplierEvaluation entity, BigDecimal explicitTotal, Integer explicitLevel) {
        BigDecimal total = explicitTotal != null ? explicitTotal
                : calcTotalScore(entity.getServiceQualityScore(),
                        entity.getFacilityQualityScore(),
                        entity.getCooperationScore(),
                        entity.getComplaintRate());
        if (total != null) {
            entity.setTotalScore(total);
        }
        Integer level = explicitLevel != null ? explicitLevel : calcScoreLevel(total);
        if (level != null) {
            entity.setScoreLevel(level);
        }
    }

    /**
     * 计算 totalScore = (s + f + c) / 3 * (1 - complaintRate/100)，保留 2 位。
     * 任一输入为 null 时返回 null。
     */
    private BigDecimal calcTotalScore(BigDecimal s, BigDecimal f, BigDecimal c, BigDecimal complaintRate) {
        if (s == null || f == null || c == null) {
            return null;
        }
        BigDecimal avg = s.add(f).add(c).divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);
        if (complaintRate != null) {
            BigDecimal factor = BigDecimal.ONE.subtract(
                    complaintRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            avg = avg.multiply(factor);
        }
        return avg.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算 scoreLevel：1=A(>=90) / 2=B(80-89) / 3=C(70-79) / 4=D(<70)。
     */
    private Integer calcScoreLevel(BigDecimal totalScore) {
        if (totalScore == null) {
            return null;
        }
        double v = totalScore.doubleValue();
        if (v >= 90) {
            return 1;
        } else if (v >= 80) {
            return 2;
        } else if (v >= 70) {
            return 3;
        } else {
            return 4;
        }
    }

    private <T> T pick(T candidate, T fallback) {
        return candidate != null ? candidate : fallback;
    }

    private SupplierEvaluationVO toVO(SupplierEvaluation entity) {
        SupplierEvaluationVO vo = new SupplierEvaluationVO();
        vo.setId(entity.getId());
        vo.setSupplierCode(entity.getSupplierCode());
        vo.setEvalPeriod(entity.getEvalPeriod());
        vo.setEvalType(entity.getEvalType());
        vo.setServiceQualityScore(entity.getServiceQualityScore());
        vo.setFacilityQualityScore(entity.getFacilityQualityScore());
        vo.setCooperationScore(entity.getCooperationScore());
        vo.setComplaintRate(entity.getComplaintRate());
        vo.setTotalOrderCount(entity.getTotalOrderCount());
        vo.setComplaintCount(entity.getComplaintCount());
        vo.setTotalScore(entity.getTotalScore());
        vo.setScoreLevel(entity.getScoreLevel());
        vo.setEvalContent(entity.getEvalContent());
        vo.setImprovementSuggestions(entity.getImprovementSuggestions());
        vo.setEvaluatorCode(entity.getEvaluatorCode());
        vo.setEvaluatorName(entity.getEvaluatorName());
        vo.setEvalDate(entity.getEvalDate());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
