package com.dayan.job.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.finance.dto.CreateReconciliationDTO;
import com.dayan.finance.dto.ReconciliationSubmitDiffDTO;
import com.dayan.finance.entity.FinanceBill;
import com.dayan.finance.entity.FinanceFlow;
import com.dayan.finance.enums.FinanceEvent;
import com.dayan.finance.mapper.FinanceBillMapper;
import com.dayan.finance.mapper.FinanceFlowMapper;
import com.dayan.finance.service.FinanceReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自动对账定时任务。
 *
 * <p>每日 03:00 执行，核对前一日的 {@code finance_flow}（明细流水）与 {@code finance_bill}（结算单）
 * 按渠道（targetCode）聚合后是否一致，差异落 {@code finance_reconciliation} 并进入"待确认"态。
 *
 * <p>对账口径（与 DDL/业务约定对齐）：
 * <ul>
 *   <li><b>我方金额</b>：{@code finance_flow.flow_amount} 之和，条件 status={@link FinanceEvent#FLOW_STATUS_NORMAL}（正常）
 *       + flow_time ∈ [昨日 00:00, 昨日 24:00) + account_type=channel + account_code=targetCode</li>
 *   <li><b>对方金额</b>：{@code finance_bill.final_amount} 之和，条件 bill_status={@link FinanceEvent#BILL_STATUS_SETTLED}（已结算）
 *       + period ∈ [昨日, 昨日] + target_type=channel + target_code=targetCode</li>
 *   <li>差异 = |我方 − 对方|；差异 = 0 → {@code complete} 0→1；否则 → {@code submitDiff} 0→2</li>
 * </ul>
 *
 * <p>逐渠道处理，单渠道失败仅记录日志、不中断整批。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    /** 对账对象类型：渠道（当前仅支持渠道对账） */
    private static final String TARGET_TYPE_CHANNEL = FinanceEvent.ACCOUNT_TYPE_CHANNEL;
    /** 对账类型：1=渠道对账 */
    private static final int RECON_TYPE_CHANNEL = 1;
    /** 系统操作人标识 */
    private static final String OPERATOR_CODE = "system";
    private static final String OPERATOR_NAME = "自动对账任务";

    private final FinanceFlowMapper financeFlowMapper;
    private final FinanceBillMapper financeBillMapper;
    private final FinanceReconciliationService financeReconciliationService;

    /**
     * 每日 03:00 执行（cron：分 时 日 月 周）。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyReconciliation() {
        long start = System.currentTimeMillis();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int matchCount = 0;
        int diffCount = 0;
        try {
            // 1. 收集昨日有结算单的所有渠道（targetCode），避免无单渠道空对账
            List<FinanceBill> bills = financeBillMapper.selectList(
                    new LambdaQueryWrapper<FinanceBill>()
                            .eq(FinanceBill::getTargetType, TARGET_TYPE_CHANNEL)
                            .eq(FinanceBill::getPeriodStart, yesterday)
                            .eq(FinanceBill::getPeriodEnd, yesterday)
                            .eq(FinanceBill::getBillStatus, FinanceEvent.BILL_STATUS_SETTLED));
            List<String> channelCodes = bills.stream()
                    .map(FinanceBill::getTargetCode)
                    .distinct()
                    .collect(Collectors.toList());
            if (channelCodes.isEmpty()) {
                log.info("[自动对账] 昨日({})无已结算渠道结算单，跳过，耗时 {}ms",
                        yesterday, System.currentTimeMillis() - start);
                return;
            }
            // 2. 逐渠道比对
            for (String channelCode : channelCodes) {
                try {
                    boolean matched = reconcileChannel(yesterday, channelCode);
                    if (matched) {
                        matchCount++;
                    } else {
                        diffCount++;
                    }
                } catch (Exception e) {
                    log.warn("[自动对账] channelCode={} 对账失败: {}", channelCode, e.getMessage());
                }
            }
            log.info("[自动对账] 对账完成，昨日({}) 渠道总数 {} / 一致 {} / 差异 {}，耗时 {}ms",
                    yesterday, channelCodes.size(), matchCount, diffCount,
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[自动对账] 执行失败", e);
        }
    }

    /**
     * 单渠道对账：聚合 flow 与 bill，比对后落 finance_reconciliation。
     *
     * @return true=一致（已完成），false=有差异（已提交待确认）
     */
    private boolean reconcileChannel(LocalDate yesterday, String channelCode) {
        // 我方金额：流水 status=1 + flow_time ∈ [昨日00:00, 昨日24:00) + account_type=channel + account_code=channelCode
        LocalDateTime flowStart = yesterday.atStartOfDay();
        LocalDateTime flowEnd = yesterday.plusDays(1).atStartOfDay();
        List<FinanceFlow> flows = financeFlowMapper.selectList(
                new LambdaQueryWrapper<FinanceFlow>()
                        .eq(FinanceFlow::getAccountType, FinanceEvent.ACCOUNT_TYPE_CHANNEL)
                        .eq(FinanceFlow::getAccountCode, channelCode)
                        .eq(FinanceFlow::getStatus, FinanceEvent.FLOW_STATUS_NORMAL)
                        .ge(FinanceFlow::getFlowTime, flowStart)
                        .lt(FinanceFlow::getFlowTime, flowEnd));
        BigDecimal ourTotal = flows.stream()
                .map(f -> f.getFlowAmount() == null ? BigDecimal.ZERO : f.getFlowAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int ourOrderCount = flows.size();

        // 对方金额：bill_status=3（已结算）+ period ∈ [昨日, 昨日] + target_code=channelCode
        List<FinanceBill> bills = financeBillMapper.selectList(
                new LambdaQueryWrapper<FinanceBill>()
                        .eq(FinanceBill::getTargetType, TARGET_TYPE_CHANNEL)
                        .eq(FinanceBill::getTargetCode, channelCode)
                        .eq(FinanceBill::getBillStatus, FinanceEvent.BILL_STATUS_SETTLED)
                        .eq(FinanceBill::getPeriodStart, yesterday)
                        .eq(FinanceBill::getPeriodEnd, yesterday));
        BigDecimal theirTotal = bills.stream()
                .map(b -> b.getFinalAmount() == null ? BigDecimal.ZERO : b.getFinalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int theirOrderCount = bills.stream()
                .mapToInt(b -> b.getOrderCount() == null ? 0 : b.getOrderCount())
                .sum();

        // 差异判定（与 DDL 注释口径一致：0=有差异/1=一致）
        int cmp = ourTotal.compareTo(theirTotal);
        boolean matched = cmp == 0;
        BigDecimal diffAmount = matched ? BigDecimal.ZERO : ourTotal.subtract(theirTotal).abs();

        // 落 finance_reconciliation（status=0 对账中）
        CreateReconciliationDTO createDTO = new CreateReconciliationDTO();
        createDTO.setReconType(RECON_TYPE_CHANNEL);
        createDTO.setTargetCode(channelCode);
        createDTO.setTargetName(channelCode); // 渠道简称暂以编码代替，无渠道表跨模块依赖
        createDTO.setPeriodStart(yesterday);
        createDTO.setPeriodEnd(yesterday);
        createDTO.setOurOrderCount(ourOrderCount);
        createDTO.setOurTotalAmount(ourTotal);
        createDTO.setTheirOrderCount(theirOrderCount);
        createDTO.setTheirTotalAmount(theirTotal);
        createDTO.setDiffCount(matched ? 0 : 1);
        createDTO.setDiffAmount(diffAmount);
        createDTO.setDiffDetail(matched ? null
                : String.format("{\"our\":%s,\"their\":%s,\"diff\":%s}",
                        ourTotal.toPlainString(), theirTotal.toPlainString(), diffAmount.toPlainString()));
        createDTO.setReconResult(matched
                ? FinanceEvent.RECON_RESULT_MATCH
                : FinanceEvent.RECON_RESULT_DIFF);
        createDTO.setOperatorCode(OPERATOR_CODE);
        createDTO.setOperatorName(OPERATOR_NAME);
        createDTO.setRemark("自动对账：" + yesterday);
        String reconCode = financeReconciliationService.create(createDTO);

        // 一致 → complete(0→1)；差异 → submitDiff(0→2)
        if (matched) {
            financeReconciliationService.complete(reconCode);
            log.info("[自动对账] channelCode={} 一致，reconCode={}，our={} their={}",
                    channelCode, reconCode, ourTotal, theirTotal);
        } else {
            ReconciliationSubmitDiffDTO diffDTO = new ReconciliationSubmitDiffDTO();
            diffDTO.setReconCode(reconCode);
            diffDTO.setDiffDetail(createDTO.getDiffDetail());
            diffDTO.setHandleResult("待人工复核");
            diffDTO.setOperatorCode(OPERATOR_CODE);
            diffDTO.setOperatorName(OPERATOR_NAME);
            diffDTO.setRemark("自动对账发现金额差异");
            financeReconciliationService.submitDiff(diffDTO);
            log.warn("[自动对账] channelCode={} 有差异，reconCode={}，our={} their={} diff={}",
                    channelCode, reconCode, ourTotal, theirTotal, diffAmount);
        }
        return matched;
    }
}
