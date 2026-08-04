package com.dayan.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.BillAuditDTO;
import com.dayan.finance.dto.BillFinishSettleDTO;
import com.dayan.finance.dto.FinanceBillQueryDTO;
import com.dayan.finance.dto.GenerateBillDTO;
import com.dayan.finance.entity.FinanceBill;
import com.dayan.finance.enums.FinanceEvent;
import com.dayan.finance.mapper.FinanceBillMapper;
import com.dayan.finance.service.FinanceBillService;
import com.dayan.finance.vo.FinanceBillVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 结算单（finance_bill）服务实现。
 *
 * <p>状态机：generate(0 待审核) → audit 0→1/0→4 → startSettle 1→2 → finishSettle 2→3。
 * 编码：BL + 10 位序号。
 *
 * <p>final_amount = total_amount - commission_amount - refund_amount + adjust_amount。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceBillServiceImpl implements FinanceBillService {

    private final FinanceBillMapper billMapper;
    private final SequenceProvider sequenceProvider;

    // ====== 查询 ======

    @Override
    public PageResult<FinanceBillVO> page(FinanceBillQueryDTO query) {
        LambdaQueryWrapper<FinanceBill> wrapper = buildQueryWrapper(query);
        Page<FinanceBill> page = billMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FinanceBillVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<FinanceBillVO> list(FinanceBillQueryDTO query) {
        return billMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public FinanceBillVO getDetail(String billCode) {
        return toVO(requireBill(billCode));
    }

    // ====== 写入 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generate(GenerateBillDTO dto) {
        BigDecimal total = nz(dto.getTotalAmount());
        BigDecimal commission = nz(dto.getCommissionAmount());
        BigDecimal refund = nz(dto.getRefundAmount());
        BigDecimal adjust = nz(dto.getAdjustAmount());
        BigDecimal finalAmount = total
                .subtract(commission)
                .subtract(refund)
                .add(adjust);

        if (dto.getPeriodStart() != null && dto.getPeriodEnd() != null
                && dto.getPeriodStart().isAfter(dto.getPeriodEnd())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结算周期开始不能晚于结束");
        }

        LocalDateTime now = LocalDateTime.now();
        FinanceBill entity = new FinanceBill();
        String billCode = generateBillCode();
        entity.setBillCode(billCode);
        entity.setBillType(dto.getBillType());
        entity.setTargetType(dto.getTargetType());
        entity.setTargetCode(dto.getTargetCode());
        entity.setTargetName(dto.getTargetName());
        entity.setPeriodStart(dto.getPeriodStart());
        entity.setPeriodEnd(dto.getPeriodEnd());
        entity.setOrderCount(dto.getOrderCount());
        entity.setTotalAmount(total);
        entity.setCommissionAmount(commission);
        entity.setRefundAmount(refund);
        entity.setAdjustAmount(adjust);
        entity.setFinalAmount(finalAmount);
        entity.setFlowIds(serializeFlowIds(dto.getFlowIds()));
        entity.setSettlementMethod(dto.getSettlementMethod() != null ? dto.getSettlementMethod() : 1);
        entity.setBankInfo(dto.getBankInfo());
        entity.setApplyTime(now);
        entity.setBillStatus(FinanceEvent.BILL_STATUS_PENDING_AUDIT);
        entity.setRemark(dto.getRemark());
        billMapper.insert(entity);

        log.info("生成结算单: billCode={}, target={}/{}, finalAmount={}",
                billCode, dto.getTargetType(), dto.getTargetCode(), finalAmount);
        return billCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(BillAuditDTO dto) {
        FinanceBill bill = requireBill(dto.getBillCode());
        int from = bill.getBillStatus() == null
                ? FinanceEvent.BILL_STATUS_PENDING_AUDIT : bill.getBillStatus();
        if (from != FinanceEvent.BILL_STATUS_PENDING_AUDIT) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "结算单非待审核状态，无法审核（当前状态=" + from + "）");
        }
        int to = Boolean.TRUE.equals(dto.getPass())
                ? FinanceEvent.BILL_STATUS_AUDIT_PASS
                : FinanceEvent.BILL_STATUS_AUDIT_REJECT;
        LocalDateTime now = LocalDateTime.now();
        FinanceBill update = new FinanceBill();
        update.setId(bill.getId());
        update.setBillStatus(to);
        update.setAuditTime(now);
        update.setAuditorCode(dto.getAuditorCode());
        update.setAuditorName(dto.getAuditorName());
        update.setAuditRemark(dto.getAuditRemark());
        billMapper.updateById(update);
        log.info("结算单审核: billCode={}, pass={}, to={}", dto.getBillCode(), dto.getPass(), to);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startSettle(String billCode) {
        FinanceBill bill = requireBill(billCode);
        int from = bill.getBillStatus() == null
                ? FinanceEvent.BILL_STATUS_PENDING_AUDIT : bill.getBillStatus();
        if (from != FinanceEvent.BILL_STATUS_AUDIT_PASS) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "结算单非审核通过状态，无法开始结算（当前状态=" + from + "）");
        }
        FinanceBill update = new FinanceBill();
        update.setId(bill.getId());
        update.setBillStatus(FinanceEvent.BILL_STATUS_SETTLING);
        billMapper.updateById(update);
        log.info("结算单开始结算: billCode={}", billCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishSettle(BillFinishSettleDTO dto) {
        FinanceBill bill = requireBill(dto.getBillCode());
        int from = bill.getBillStatus() == null
                ? FinanceEvent.BILL_STATUS_PENDING_AUDIT : bill.getBillStatus();
        if (from != FinanceEvent.BILL_STATUS_SETTLING) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "结算单非结算中状态，无法完成结算（当前状态=" + from + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        FinanceBill update = new FinanceBill();
        update.setId(bill.getId());
        update.setBillStatus(FinanceEvent.BILL_STATUS_SETTLED);
        update.setSettleTime(dto.getSettleTime() != null ? dto.getSettleTime() : now);
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        billMapper.updateById(update);
        log.info("结算单完成结算: billCode={}", dto.getBillCode());
    }

    // ====== 内部方法 ======

    @Override
    public FinanceBill requireBill(String billCode) {
        FinanceBill bill = billMapper.selectOne(new LambdaQueryWrapper<FinanceBill>()
                .eq(FinanceBill::getBillCode, billCode)
                .last("LIMIT 1"));
        if (bill == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "结算单不存在: " + billCode);
        }
        return bill;
    }

    /** null → BigDecimal.ZERO */
    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** flowIds 列表序列化为 JSON 数组字符串（无则 null） */
    private static String serializeFlowIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(ids.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateBillCode() {
        long seq = sequenceProvider.next(FinanceEvent.BILL_SEQ_KEY);
        return FinanceEvent.BILL_PREFIX + String.format("%0" + FinanceEvent.BILL_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<FinanceBill> buildQueryWrapper(FinanceBillQueryDTO query) {
        LambdaQueryWrapper<FinanceBill> wrapper = new LambdaQueryWrapper<FinanceBill>()
                .orderByDesc(FinanceBill::getCreatedAt);
        if (query.getBillCode() != null && !query.getBillCode().isEmpty()) {
            wrapper.eq(FinanceBill::getBillCode, query.getBillCode());
        }
        if (query.getBillType() != null) {
            wrapper.eq(FinanceBill::getBillType, query.getBillType());
        }
        if (query.getTargetType() != null && !query.getTargetType().isEmpty()) {
            wrapper.eq(FinanceBill::getTargetType, query.getTargetType());
        }
        if (query.getTargetCode() != null && !query.getTargetCode().isEmpty()) {
            wrapper.eq(FinanceBill::getTargetCode, query.getTargetCode());
        }
        if (query.getBillStatus() != null) {
            wrapper.eq(FinanceBill::getBillStatus, query.getBillStatus());
        }
        if (query.getPeriodStartFrom() != null) {
            wrapper.ge(FinanceBill::getPeriodStart, query.getPeriodStartFrom());
        }
        if (query.getPeriodEndTo() != null) {
            wrapper.le(FinanceBill::getPeriodEnd, query.getPeriodEndTo());
        }
        return wrapper;
    }

    private FinanceBillVO toVO(FinanceBill entity) {
        FinanceBillVO vo = new FinanceBillVO();
        vo.setId(entity.getId());
        vo.setBillCode(entity.getBillCode());
        vo.setBillType(entity.getBillType());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetCode(entity.getTargetCode());
        vo.setTargetName(entity.getTargetName());
        vo.setPeriodStart(entity.getPeriodStart());
        vo.setPeriodEnd(entity.getPeriodEnd());
        vo.setOrderCount(entity.getOrderCount());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setCommissionAmount(entity.getCommissionAmount());
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setAdjustAmount(entity.getAdjustAmount());
        vo.setFinalAmount(entity.getFinalAmount());
        vo.setFlowIds(entity.getFlowIds());
        vo.setSettlementMethod(entity.getSettlementMethod());
        vo.setBankInfo(entity.getBankInfo());
        vo.setApplyTime(entity.getApplyTime());
        vo.setAuditTime(entity.getAuditTime());
        vo.setSettleTime(entity.getSettleTime());
        vo.setAuditorCode(entity.getAuditorCode());
        vo.setAuditorName(entity.getAuditorName());
        vo.setAuditRemark(entity.getAuditRemark());
        vo.setBillStatus(entity.getBillStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
