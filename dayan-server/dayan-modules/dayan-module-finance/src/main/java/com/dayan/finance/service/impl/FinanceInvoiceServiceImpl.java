package com.dayan.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.ApplyInvoiceDTO;
import com.dayan.finance.dto.FinanceInvoiceQueryDTO;
import com.dayan.finance.dto.InvoiceAuditDTO;
import com.dayan.finance.dto.InvoiceIssueDTO;
import com.dayan.finance.dto.InvoiceOperateDTO;
import com.dayan.finance.dto.InvoiceSendDTO;
import com.dayan.finance.entity.FinanceInvoice;
import com.dayan.finance.enums.FinanceEvent;
import com.dayan.finance.mapper.FinanceInvoiceMapper;
import com.dayan.finance.service.FinanceInvoiceService;
import com.dayan.finance.vo.FinanceInvoiceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 发票（finance_invoice）服务实现。
 *
 * <p>状态机：apply(0 待审核) → audit 0→1 → issue 1→2 → send 2→3 → finish 3→4；
 * voidInvoice →5（作废）；redFlush →6（红冲）。
 * 编码：IV + 10 位序号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceInvoiceServiceImpl implements FinanceInvoiceService {

    private final FinanceInvoiceMapper invoiceMapper;
    private final SequenceProvider sequenceProvider;

    // ====== 查询 ======

    @Override
    public PageResult<FinanceInvoiceVO> page(FinanceInvoiceQueryDTO query) {
        LambdaQueryWrapper<FinanceInvoice> wrapper = buildQueryWrapper(query);
        Page<FinanceInvoice> page = invoiceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FinanceInvoiceVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<FinanceInvoiceVO> list(FinanceInvoiceQueryDTO query) {
        return invoiceMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public FinanceInvoiceVO getDetail(String invoiceCode) {
        return toVO(requireInvoice(invoiceCode));
    }

    // ====== 写入 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String apply(ApplyInvoiceDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        FinanceInvoice entity = new FinanceInvoice();
        String invoiceCode = generateInvoiceCode();
        entity.setInvoiceCode(invoiceCode);
        entity.setInvoiceType(dto.getInvoiceType());
        entity.setBillCode(dto.getBillCode());
        entity.setOrderCode(dto.getOrderCode());
        entity.setApplicantType(dto.getApplicantType());
        entity.setApplicantCode(dto.getApplicantCode());
        entity.setApplicantName(dto.getApplicantName());
        entity.setTitleType(dto.getTitleType() != null ? dto.getTitleType() : 1);
        entity.setInvoiceTitle(dto.getInvoiceTitle());
        entity.setTaxNo(dto.getTaxNo());
        entity.setBankName(dto.getBankName());
        entity.setBankAccount(dto.getBankAccount());
        entity.setRegisterAddress(dto.getRegisterAddress());
        entity.setRegisterPhone(dto.getRegisterPhone());
        entity.setInvoiceAmount(dto.getInvoiceAmount());
        entity.setInvoiceContent(dto.getInvoiceContent());
        entity.setReceiverName(dto.getReceiverName());
        entity.setReceiverPhone(dto.getReceiverPhone());
        entity.setReceiverAddress(dto.getReceiverAddress());
        entity.setReceiverEmail(dto.getReceiverEmail());
        entity.setApplyTime(now);
        entity.setInvoiceStatus(FinanceEvent.INVOICE_STATUS_PENDING_AUDIT);
        entity.setRemark(dto.getRemark());
        invoiceMapper.insert(entity);

        log.info("申请发票: invoiceCode={}, applicant={}/{}, amount={}",
                invoiceCode, dto.getApplicantType(), dto.getApplicantCode(), dto.getInvoiceAmount());
        return invoiceCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(InvoiceAuditDTO dto) {
        transitionState(dto.getInvoiceCode(), FinanceEvent.INVOICE_STATUS_PENDING_AUDIT,
                FinanceEvent.INVOICE_STATUS_AUDITED, "审核", dto.getRemark(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(InvoiceIssueDTO dto) {
        FinanceInvoice invoice = requireInvoice(dto.getInvoiceCode());
        int from = invoice.getInvoiceStatus() == null
                ? FinanceEvent.INVOICE_STATUS_PENDING_AUDIT : invoice.getInvoiceStatus();
        if (from != FinanceEvent.INVOICE_STATUS_AUDITED) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "发票非已审核状态，无法开票（当前状态=" + from + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        FinanceInvoice update = new FinanceInvoice();
        update.setId(invoice.getId());
        update.setInvoiceStatus(FinanceEvent.INVOICE_STATUS_ISSUED);
        update.setInvoiceNo(dto.getInvoiceNo());
        update.setIssueTime(now);
        if (dto.getInvoiceUrl() != null && !dto.getInvoiceUrl().isEmpty()) {
            update.setInvoiceUrl(dto.getInvoiceUrl());
        }
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        invoiceMapper.updateById(update);
        log.info("发票开票: invoiceCode={}, invoiceNo={}", dto.getInvoiceCode(), dto.getInvoiceNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(InvoiceSendDTO dto) {
        FinanceInvoice invoice = requireInvoice(dto.getInvoiceCode());
        int from = invoice.getInvoiceStatus() == null
                ? FinanceEvent.INVOICE_STATUS_PENDING_AUDIT : invoice.getInvoiceStatus();
        if (from != FinanceEvent.INVOICE_STATUS_ISSUED) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "发票非已开票状态，无法寄出（当前状态=" + from + "）");
        }
        FinanceInvoice update = new FinanceInvoice();
        update.setId(invoice.getId());
        update.setInvoiceStatus(FinanceEvent.INVOICE_STATUS_SENT);
        update.setSendTime(LocalDateTime.now());
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        invoiceMapper.updateById(update);
        log.info("发票寄出: invoiceCode={}", dto.getInvoiceCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(InvoiceOperateDTO dto) {
        transitionState(dto.getInvoiceCode(), FinanceEvent.INVOICE_STATUS_SENT,
                FinanceEvent.INVOICE_STATUS_DONE, "完成", dto.getRemark(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidInvoice(InvoiceOperateDTO dto) {
        FinanceInvoice invoice = requireInvoice(dto.getInvoiceCode());
        int from = invoice.getInvoiceStatus() == null
                ? FinanceEvent.INVOICE_STATUS_PENDING_AUDIT : invoice.getInvoiceStatus();
        // 作废：仅已开票相关状态可作废（已开/已寄/已完成）
        if (from != FinanceEvent.INVOICE_STATUS_ISSUED
                && from != FinanceEvent.INVOICE_STATUS_SENT
                && from != FinanceEvent.INVOICE_STATUS_DONE) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "发票当前状态不允许作废（当前状态=" + from + "）");
        }
        FinanceInvoice update = new FinanceInvoice();
        update.setId(invoice.getId());
        update.setInvoiceStatus(FinanceEvent.INVOICE_STATUS_VOID);
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        invoiceMapper.updateById(update);
        log.info("发票作废: invoiceCode={}", dto.getInvoiceCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redFlush(InvoiceOperateDTO dto) {
        FinanceInvoice invoice = requireInvoice(dto.getInvoiceCode());
        int from = invoice.getInvoiceStatus() == null
                ? FinanceEvent.INVOICE_STATUS_PENDING_AUDIT : invoice.getInvoiceStatus();
        // 红冲：仅已完成（4）或已寄出（3）可红冲
        if (from != FinanceEvent.INVOICE_STATUS_DONE && from != FinanceEvent.INVOICE_STATUS_SENT) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "发票当前状态不允许红冲（当前状态=" + from + "）");
        }
        FinanceInvoice update = new FinanceInvoice();
        update.setId(invoice.getId());
        update.setInvoiceStatus(FinanceEvent.INVOICE_STATUS_RED_FLUSH);
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        invoiceMapper.updateById(update);
        log.info("发票红冲: invoiceCode={}", dto.getInvoiceCode());
    }

    // ====== 内部方法 ======

    @Override
    public FinanceInvoice requireInvoice(String invoiceCode) {
        FinanceInvoice invoice = invoiceMapper.selectOne(new LambdaQueryWrapper<FinanceInvoice>()
                .eq(FinanceInvoice::getInvoiceCode, invoiceCode)
                .last("LIMIT 1"));
        if (invoice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发票不存在: " + invoiceCode);
        }
        return invoice;
    }

    /** 通用单状态流转：校验 from 后置 to */
    private void transitionState(String invoiceCode, int expectFrom, int to,
                                 String actionName, String remark, String ignore) {
        FinanceInvoice invoice = requireInvoice(invoiceCode);
        int from = invoice.getInvoiceStatus() == null
                ? FinanceEvent.INVOICE_STATUS_PENDING_AUDIT : invoice.getInvoiceStatus();
        if (from != expectFrom) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "发票非预期状态，无法" + actionName + "（期望=" + expectFrom + "，当前=" + from + "）");
        }
        FinanceInvoice update = new FinanceInvoice();
        update.setId(invoice.getId());
        update.setInvoiceStatus(to);
        if (remark != null && !remark.isEmpty()) {
            update.setRemark(remark);
        }
        invoiceMapper.updateById(update);
        log.info("发票{}: invoiceCode={}, {}->{}", actionName, invoiceCode, from, to);
    }

    private String generateInvoiceCode() {
        long seq = sequenceProvider.next(FinanceEvent.INVOICE_SEQ_KEY);
        return FinanceEvent.INVOICE_PREFIX + String.format("%0" + FinanceEvent.INVOICE_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<FinanceInvoice> buildQueryWrapper(FinanceInvoiceQueryDTO query) {
        LambdaQueryWrapper<FinanceInvoice> wrapper = new LambdaQueryWrapper<FinanceInvoice>()
                .orderByDesc(FinanceInvoice::getCreatedAt);
        if (query.getInvoiceCode() != null && !query.getInvoiceCode().isEmpty()) {
            wrapper.eq(FinanceInvoice::getInvoiceCode, query.getInvoiceCode());
        }
        if (query.getInvoiceType() != null) {
            wrapper.eq(FinanceInvoice::getInvoiceType, query.getInvoiceType());
        }
        if (query.getBillCode() != null && !query.getBillCode().isEmpty()) {
            wrapper.eq(FinanceInvoice::getBillCode, query.getBillCode());
        }
        if (query.getOrderCode() != null && !query.getOrderCode().isEmpty()) {
            wrapper.eq(FinanceInvoice::getOrderCode, query.getOrderCode());
        }
        if (query.getApplicantType() != null && !query.getApplicantType().isEmpty()) {
            wrapper.eq(FinanceInvoice::getApplicantType, query.getApplicantType());
        }
        if (query.getApplicantCode() != null && !query.getApplicantCode().isEmpty()) {
            wrapper.eq(FinanceInvoice::getApplicantCode, query.getApplicantCode());
        }
        if (query.getInvoiceNo() != null && !query.getInvoiceNo().isEmpty()) {
            wrapper.eq(FinanceInvoice::getInvoiceNo, query.getInvoiceNo());
        }
        if (query.getInvoiceStatus() != null) {
            wrapper.eq(FinanceInvoice::getInvoiceStatus, query.getInvoiceStatus());
        }
        return wrapper;
    }

    private FinanceInvoiceVO toVO(FinanceInvoice entity) {
        FinanceInvoiceVO vo = new FinanceInvoiceVO();
        vo.setId(entity.getId());
        vo.setInvoiceCode(entity.getInvoiceCode());
        vo.setInvoiceType(entity.getInvoiceType());
        vo.setBillCode(entity.getBillCode());
        vo.setOrderCode(entity.getOrderCode());
        vo.setApplicantType(entity.getApplicantType());
        vo.setApplicantCode(entity.getApplicantCode());
        vo.setApplicantName(entity.getApplicantName());
        vo.setTitleType(entity.getTitleType());
        vo.setInvoiceTitle(entity.getInvoiceTitle());
        vo.setTaxNo(entity.getTaxNo());
        vo.setBankName(entity.getBankName());
        vo.setBankAccount(entity.getBankAccount());
        vo.setRegisterAddress(entity.getRegisterAddress());
        vo.setRegisterPhone(entity.getRegisterPhone());
        vo.setInvoiceAmount(entity.getInvoiceAmount());
        vo.setInvoiceContent(entity.getInvoiceContent());
        vo.setReceiverName(entity.getReceiverName());
        vo.setReceiverPhone(entity.getReceiverPhone());
        vo.setReceiverAddress(entity.getReceiverAddress());
        vo.setReceiverEmail(entity.getReceiverEmail());
        vo.setInvoiceNo(entity.getInvoiceNo());
        vo.setInvoiceUrl(entity.getInvoiceUrl());
        vo.setApplyTime(entity.getApplyTime());
        vo.setIssueTime(entity.getIssueTime());
        vo.setSendTime(entity.getSendTime());
        vo.setInvoiceStatus(entity.getInvoiceStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
