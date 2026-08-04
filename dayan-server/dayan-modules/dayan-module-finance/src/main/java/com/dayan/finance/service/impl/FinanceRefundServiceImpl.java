package com.dayan.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.ApplyRefundDTO;
import com.dayan.finance.dto.FinanceRefundQueryDTO;
import com.dayan.finance.dto.RefundAuditDTO;
import com.dayan.finance.dto.RefundMarkFailedDTO;
import com.dayan.finance.dto.RefundMarkSuccessDTO;
import com.dayan.finance.entity.FinanceRefund;
import com.dayan.finance.enums.FinanceEvent;
import com.dayan.finance.mapper.FinanceRefundMapper;
import com.dayan.finance.service.FinanceRefundService;
import com.dayan.finance.vo.FinanceRefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单退款记录（finance_refund）服务实现。
 *
 * <p>状态机：apply(0 待审核) → audit 0→1/0→4 → markRefunding 1→2 → markSuccess 2→3 / markFailed 2→5。
 * 编码：RF + 10 位序号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceRefundServiceImpl implements FinanceRefundService {

    private final FinanceRefundMapper refundMapper;
    private final SequenceProvider sequenceProvider;

    // ====== 查询 ======

    @Override
    public PageResult<FinanceRefundVO> page(FinanceRefundQueryDTO query) {
        LambdaQueryWrapper<FinanceRefund> wrapper = buildQueryWrapper(query);
        Page<FinanceRefund> page = refundMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FinanceRefundVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<FinanceRefundVO> list(FinanceRefundQueryDTO query) {
        return refundMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public FinanceRefundVO getDetail(String refundCode) {
        return toVO(requireRefund(refundCode));
    }

    // ====== 写入 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String apply(ApplyRefundDTO dto) {
        if (dto.getRefundAmount() == null || dto.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "退款金额必须 > 0");
        }
        LocalDateTime now = LocalDateTime.now();

        FinanceRefund entity = new FinanceRefund();
        String refundCode = generateRefundCode();
        entity.setRefundCode(refundCode);
        entity.setOrderType(dto.getOrderType());
        entity.setOrderCode(dto.getOrderCode());
        entity.setPaymentCode(dto.getPaymentCode());
        entity.setRefundAmount(dto.getRefundAmount());
        entity.setRefundReason(dto.getRefundReason());
        entity.setRefundType(dto.getRefundType() != null ? dto.getRefundType() : 1);
        entity.setRefundChannel(dto.getRefundChannel() != null ? dto.getRefundChannel() : 1);
        entity.setApplyTime(now);
        entity.setRefundStatus(FinanceEvent.REFUND_STATUS_PENDING_AUDIT);
        entity.setRemark(dto.getRemark());
        refundMapper.insert(entity);

        log.info("申请退款: refundCode={}, orderCode={}, amount={}",
                refundCode, dto.getOrderCode(), dto.getRefundAmount());
        return refundCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(RefundAuditDTO dto) {
        FinanceRefund refund = requireRefund(dto.getRefundCode());
        int from = refund.getRefundStatus() == null
                ? FinanceEvent.REFUND_STATUS_PENDING_AUDIT : refund.getRefundStatus();
        if (from != FinanceEvent.REFUND_STATUS_PENDING_AUDIT) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "退款记录非待审核状态，无法审核（当前状态=" + from + "）");
        }
        int to = Boolean.TRUE.equals(dto.getPass())
                ? FinanceEvent.REFUND_STATUS_AUDIT_PASS
                : FinanceEvent.REFUND_STATUS_AUDIT_REJECT;
        LocalDateTime now = LocalDateTime.now();
        FinanceRefund update = new FinanceRefund();
        update.setId(refund.getId());
        update.setRefundStatus(to);
        update.setAuditTime(now);
        update.setAuditorCode(dto.getAuditorCode());
        update.setAuditorName(dto.getAuditorName());
        update.setAuditRemark(dto.getAuditRemark());
        refundMapper.updateById(update);
        log.info("退款审核: refundCode={}, pass={}, to={}", dto.getRefundCode(), dto.getPass(), to);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRefunding(String refundCode) {
        FinanceRefund refund = requireRefund(refundCode);
        int from = refund.getRefundStatus() == null
                ? FinanceEvent.REFUND_STATUS_PENDING_AUDIT : refund.getRefundStatus();
        if (from != FinanceEvent.REFUND_STATUS_AUDIT_PASS) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "退款记录非审核通过状态，无法进入退款中（当前状态=" + from + "）");
        }
        FinanceRefund update = new FinanceRefund();
        update.setId(refund.getId());
        update.setRefundStatus(FinanceEvent.REFUND_STATUS_REFUNDING);
        refundMapper.updateById(update);
        log.info("退款进入退款中: refundCode={}", refundCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(RefundMarkSuccessDTO dto) {
        FinanceRefund refund = requireRefund(dto.getRefundCode());
        int from = refund.getRefundStatus() == null
                ? FinanceEvent.REFUND_STATUS_PENDING_AUDIT : refund.getRefundStatus();
        if (from != FinanceEvent.REFUND_STATUS_REFUNDING) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "退款记录非退款中状态，无法标记成功（当前状态=" + from + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        FinanceRefund update = new FinanceRefund();
        update.setId(refund.getId());
        update.setRefundStatus(FinanceEvent.REFUND_STATUS_SUCCESS);
        update.setRefundTradeNo(dto.getRefundTradeNo());
        update.setRefundTime(dto.getRefundTime() != null ? dto.getRefundTime() : now);
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        refundMapper.updateById(update);
        log.info("退款成功: refundCode={}, tradeNo={}", dto.getRefundCode(), dto.getRefundTradeNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(RefundMarkFailedDTO dto) {
        FinanceRefund refund = requireRefund(dto.getRefundCode());
        int from = refund.getRefundStatus() == null
                ? FinanceEvent.REFUND_STATUS_PENDING_AUDIT : refund.getRefundStatus();
        if (from != FinanceEvent.REFUND_STATUS_REFUNDING) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "退款记录非退款中状态，无法标记失败（当前状态=" + from + "）");
        }
        FinanceRefund update = new FinanceRefund();
        update.setId(refund.getId());
        update.setRefundStatus(FinanceEvent.REFUND_STATUS_FAILED);
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        refundMapper.updateById(update);
        log.info("退款失败: refundCode={}", dto.getRefundCode());
    }

    // ====== 内部方法 ======

    @Override
    public FinanceRefund requireRefund(String refundCode) {
        FinanceRefund refund = refundMapper.selectOne(new LambdaQueryWrapper<FinanceRefund>()
                .eq(FinanceRefund::getRefundCode, refundCode)
                .last("LIMIT 1"));
        if (refund == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "退款记录不存在: " + refundCode);
        }
        return refund;
    }

    private String generateRefundCode() {
        long seq = sequenceProvider.next(FinanceEvent.REFUND_SEQ_KEY);
        return FinanceEvent.REFUND_PREFIX + String.format("%0" + FinanceEvent.REFUND_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<FinanceRefund> buildQueryWrapper(FinanceRefundQueryDTO query) {
        LambdaQueryWrapper<FinanceRefund> wrapper = new LambdaQueryWrapper<FinanceRefund>()
                .orderByDesc(FinanceRefund::getCreatedAt);
        if (query.getRefundCode() != null && !query.getRefundCode().isEmpty()) {
            wrapper.eq(FinanceRefund::getRefundCode, query.getRefundCode());
        }
        if (query.getOrderType() != null) {
            wrapper.eq(FinanceRefund::getOrderType, query.getOrderType());
        }
        if (query.getOrderCode() != null && !query.getOrderCode().isEmpty()) {
            wrapper.eq(FinanceRefund::getOrderCode, query.getOrderCode());
        }
        if (query.getPaymentCode() != null && !query.getPaymentCode().isEmpty()) {
            wrapper.eq(FinanceRefund::getPaymentCode, query.getPaymentCode());
        }
        if (query.getRefundType() != null) {
            wrapper.eq(FinanceRefund::getRefundType, query.getRefundType());
        }
        if (query.getRefundChannel() != null) {
            wrapper.eq(FinanceRefund::getRefundChannel, query.getRefundChannel());
        }
        if (query.getRefundStatus() != null) {
            wrapper.eq(FinanceRefund::getRefundStatus, query.getRefundStatus());
        }
        if (query.getApplyTimeFrom() != null) {
            wrapper.ge(FinanceRefund::getApplyTime, query.getApplyTimeFrom());
        }
        if (query.getApplyTimeTo() != null) {
            wrapper.le(FinanceRefund::getApplyTime, query.getApplyTimeTo());
        }
        return wrapper;
    }

    private FinanceRefundVO toVO(FinanceRefund entity) {
        FinanceRefundVO vo = new FinanceRefundVO();
        vo.setId(entity.getId());
        vo.setRefundCode(entity.getRefundCode());
        vo.setOrderType(entity.getOrderType());
        vo.setOrderCode(entity.getOrderCode());
        vo.setPaymentCode(entity.getPaymentCode());
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setRefundReason(entity.getRefundReason());
        vo.setRefundType(entity.getRefundType());
        vo.setRefundChannel(entity.getRefundChannel());
        vo.setRefundTradeNo(entity.getRefundTradeNo());
        vo.setApplyTime(entity.getApplyTime());
        vo.setAuditTime(entity.getAuditTime());
        vo.setRefundTime(entity.getRefundTime());
        vo.setAuditorCode(entity.getAuditorCode());
        vo.setAuditorName(entity.getAuditorName());
        vo.setAuditRemark(entity.getAuditRemark());
        vo.setRefundStatus(entity.getRefundStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
