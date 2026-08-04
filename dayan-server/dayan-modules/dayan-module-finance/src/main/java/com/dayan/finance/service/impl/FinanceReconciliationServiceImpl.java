package com.dayan.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.CreateReconciliationDTO;
import com.dayan.finance.dto.FinanceReconciliationQueryDTO;
import com.dayan.finance.dto.ReconciliationConfirmDTO;
import com.dayan.finance.dto.ReconciliationSubmitDiffDTO;
import com.dayan.finance.entity.FinanceReconciliation;
import com.dayan.finance.enums.FinanceEvent;
import com.dayan.finance.mapper.FinanceReconciliationMapper;
import com.dayan.finance.service.FinanceReconciliationService;
import com.dayan.finance.vo.FinanceReconciliationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对账记录（finance_reconciliation）服务实现。
 *
 * <p>状态机：create(0 对账中) → complete 0→1 / submitDiff 0→2（待确认） → confirm 2→3（已确认）。
 * 编码：RC + 10 位序号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceReconciliationServiceImpl implements FinanceReconciliationService {

    private final FinanceReconciliationMapper reconMapper;
    private final SequenceProvider sequenceProvider;

    // ====== 查询 ======

    @Override
    public PageResult<FinanceReconciliationVO> page(FinanceReconciliationQueryDTO query) {
        LambdaQueryWrapper<FinanceReconciliation> wrapper = buildQueryWrapper(query);
        Page<FinanceReconciliation> page = reconMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FinanceReconciliationVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<FinanceReconciliationVO> list(FinanceReconciliationQueryDTO query) {
        return reconMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public FinanceReconciliationVO getDetail(String reconCode) {
        return toVO(requireReconciliation(reconCode));
    }

    // ====== 写入 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateReconciliationDTO dto) {
        if (dto.getPeriodStart() != null && dto.getPeriodEnd() != null
                && dto.getPeriodStart().isAfter(dto.getPeriodEnd())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "对账周期开始不能晚于结束");
        }
        LocalDateTime now = LocalDateTime.now();
        FinanceReconciliation entity = new FinanceReconciliation();
        String reconCode = generateReconCode();
        entity.setReconCode(reconCode);
        entity.setReconType(dto.getReconType());
        entity.setTargetCode(dto.getTargetCode());
        entity.setTargetName(dto.getTargetName());
        entity.setPeriodStart(dto.getPeriodStart());
        entity.setPeriodEnd(dto.getPeriodEnd());
        entity.setOurOrderCount(dto.getOurOrderCount());
        entity.setOurTotalAmount(dto.getOurTotalAmount());
        entity.setTheirOrderCount(dto.getTheirOrderCount());
        entity.setTheirTotalAmount(dto.getTheirTotalAmount());
        entity.setDiffCount(dto.getDiffCount() != null ? dto.getDiffCount() : 0);
        entity.setDiffAmount(dto.getDiffAmount() != null ? dto.getDiffAmount() : BigDecimal.ZERO);
        entity.setDiffDetail(dto.getDiffDetail());
        entity.setReconResult(dto.getReconResult() != null ? dto.getReconResult() : FinanceEvent.RECON_RESULT_DIFF);
        entity.setReconTime(dto.getReconTime() != null ? dto.getReconTime() : now);
        entity.setOperatorCode(dto.getOperatorCode());
        entity.setOperatorName(dto.getOperatorName());
        entity.setStatus(FinanceEvent.RECON_STATUS_DOING);
        entity.setRemark(dto.getRemark());
        reconMapper.insert(entity);

        log.info("创建对账记录: reconCode={}, target={}/{}, reconResult={}",
                reconCode, dto.getTargetCode(), dto.getTargetName(), entity.getReconResult());
        return reconCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(String reconCode) {
        FinanceReconciliation recon = requireReconciliation(reconCode);
        int from = recon.getStatus() == null ? FinanceEvent.RECON_STATUS_DOING : recon.getStatus();
        if (from != FinanceEvent.RECON_STATUS_DOING) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "对账记录非对账中状态，无法完成（当前状态=" + from + "）");
        }
        FinanceReconciliation update = new FinanceReconciliation();
        update.setId(recon.getId());
        update.setStatus(FinanceEvent.RECON_STATUS_DONE);
        // 完成对账时，若 reconResult 未显式置为一致，则自动置一致
        if (recon.getReconResult() == null
                || recon.getReconResult() == FinanceEvent.RECON_RESULT_DIFF) {
            update.setReconResult(FinanceEvent.RECON_RESULT_MATCH);
        }
        reconMapper.updateById(update);
        log.info("对账完成: reconCode={}", reconCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDiff(ReconciliationSubmitDiffDTO dto) {
        FinanceReconciliation recon = requireReconciliation(dto.getReconCode());
        int from = recon.getStatus() == null ? FinanceEvent.RECON_STATUS_DOING : recon.getStatus();
        if (from != FinanceEvent.RECON_STATUS_DOING) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "对账记录非对账中状态，无法提交差异（当前状态=" + from + "）");
        }
        FinanceReconciliation update = new FinanceReconciliation();
        update.setId(recon.getId());
        update.setStatus(FinanceEvent.RECON_STATUS_PENDING_CONFIRM);
        if (dto.getDiffDetail() != null && !dto.getDiffDetail().isEmpty()) {
            update.setDiffDetail(dto.getDiffDetail());
        }
        if (dto.getHandleResult() != null && !dto.getHandleResult().isEmpty()) {
            update.setHandleResult(dto.getHandleResult());
        }
        if (dto.getOperatorCode() != null && !dto.getOperatorCode().isEmpty()) {
            update.setOperatorCode(dto.getOperatorCode());
        }
        if (dto.getOperatorName() != null && !dto.getOperatorName().isEmpty()) {
            update.setOperatorName(dto.getOperatorName());
        }
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        reconMapper.updateById(update);
        log.info("对账提交差异: reconCode={}", dto.getReconCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(ReconciliationConfirmDTO dto) {
        FinanceReconciliation recon = requireReconciliation(dto.getReconCode());
        int from = recon.getStatus() == null ? FinanceEvent.RECON_STATUS_DOING : recon.getStatus();
        if (from != FinanceEvent.RECON_STATUS_PENDING_CONFIRM) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "对账记录非待确认状态，无法确认（当前状态=" + from + "）");
        }
        FinanceReconciliation update = new FinanceReconciliation();
        update.setId(recon.getId());
        update.setStatus(FinanceEvent.RECON_STATUS_CONFIRMED);
        if (dto.getHandleResult() != null && !dto.getHandleResult().isEmpty()) {
            update.setHandleResult(dto.getHandleResult());
        }
        if (dto.getOperatorCode() != null && !dto.getOperatorCode().isEmpty()) {
            update.setOperatorCode(dto.getOperatorCode());
        }
        if (dto.getOperatorName() != null && !dto.getOperatorName().isEmpty()) {
            update.setOperatorName(dto.getOperatorName());
        }
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        reconMapper.updateById(update);
        log.info("对账确认: reconCode={}", dto.getReconCode());
    }

    // ====== 内部方法 ======

    @Override
    public FinanceReconciliation requireReconciliation(String reconCode) {
        FinanceReconciliation recon = reconMapper.selectOne(new LambdaQueryWrapper<FinanceReconciliation>()
                .eq(FinanceReconciliation::getReconCode, reconCode)
                .last("LIMIT 1"));
        if (recon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对账记录不存在: " + reconCode);
        }
        return recon;
    }

    private String generateReconCode() {
        long seq = sequenceProvider.next(FinanceEvent.RECON_SEQ_KEY);
        return FinanceEvent.RECON_PREFIX + String.format("%0" + FinanceEvent.RECON_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<FinanceReconciliation> buildQueryWrapper(FinanceReconciliationQueryDTO query) {
        LambdaQueryWrapper<FinanceReconciliation> wrapper = new LambdaQueryWrapper<FinanceReconciliation>()
                .orderByDesc(FinanceReconciliation::getCreatedAt);
        if (query.getReconCode() != null && !query.getReconCode().isEmpty()) {
            wrapper.eq(FinanceReconciliation::getReconCode, query.getReconCode());
        }
        if (query.getReconType() != null) {
            wrapper.eq(FinanceReconciliation::getReconType, query.getReconType());
        }
        if (query.getTargetCode() != null && !query.getTargetCode().isEmpty()) {
            wrapper.eq(FinanceReconciliation::getTargetCode, query.getTargetCode());
        }
        if (query.getReconResult() != null) {
            wrapper.eq(FinanceReconciliation::getReconResult, query.getReconResult());
        }
        if (query.getStatus() != null) {
            wrapper.eq(FinanceReconciliation::getStatus, query.getStatus());
        }
        if (query.getPeriodStartFrom() != null) {
            wrapper.ge(FinanceReconciliation::getPeriodStart, query.getPeriodStartFrom());
        }
        if (query.getPeriodEndTo() != null) {
            wrapper.le(FinanceReconciliation::getPeriodEnd, query.getPeriodEndTo());
        }
        return wrapper;
    }

    private FinanceReconciliationVO toVO(FinanceReconciliation entity) {
        FinanceReconciliationVO vo = new FinanceReconciliationVO();
        vo.setId(entity.getId());
        vo.setReconCode(entity.getReconCode());
        vo.setReconType(entity.getReconType());
        vo.setTargetCode(entity.getTargetCode());
        vo.setTargetName(entity.getTargetName());
        vo.setPeriodStart(entity.getPeriodStart());
        vo.setPeriodEnd(entity.getPeriodEnd());
        vo.setOurOrderCount(entity.getOurOrderCount());
        vo.setOurTotalAmount(entity.getOurTotalAmount());
        vo.setTheirOrderCount(entity.getTheirOrderCount());
        vo.setTheirTotalAmount(entity.getTheirTotalAmount());
        vo.setDiffCount(entity.getDiffCount());
        vo.setDiffAmount(entity.getDiffAmount());
        vo.setDiffDetail(entity.getDiffDetail());
        vo.setReconResult(entity.getReconResult());
        vo.setHandleResult(entity.getHandleResult());
        vo.setReconTime(entity.getReconTime());
        vo.setOperatorCode(entity.getOperatorCode());
        vo.setOperatorName(entity.getOperatorName());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
