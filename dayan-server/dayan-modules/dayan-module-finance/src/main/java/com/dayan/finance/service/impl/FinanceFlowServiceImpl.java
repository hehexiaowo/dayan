package com.dayan.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.FinanceFlowQueryDTO;
import com.dayan.finance.dto.RecordFlowDTO;
import com.dayan.finance.entity.FinanceFlow;
import com.dayan.finance.enums.FinanceEvent;
import com.dayan.finance.mapper.FinanceFlowMapper;
import com.dayan.finance.service.FinanceFlowService;
import com.dayan.finance.vo.FinanceFlowVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 财务流水（finance_flow）服务实现。
 *
 * <p>结算域不走状态机，flow.status 直接 if-else 校验取值范围。
 * 编码生成：FL + 10 位序号（{@link FinanceEvent#FLOW_SEQ_KEY}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceFlowServiceImpl implements FinanceFlowService {

    private final FinanceFlowMapper flowMapper;
    private final SequenceProvider sequenceProvider;

    // ====== 查询 ======

    @Override
    public PageResult<FinanceFlowVO> page(FinanceFlowQueryDTO query) {
        LambdaQueryWrapper<FinanceFlow> wrapper = buildQueryWrapper(query);
        Page<FinanceFlow> page = flowMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FinanceFlowVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<FinanceFlowVO> list(FinanceFlowQueryDTO query) {
        return flowMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public FinanceFlowVO getDetail(String flowCode) {
        return toVO(requireFlow(flowCode));
    }

    // ====== 写入 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String record(RecordFlowDTO dto) {
        // 校验 flowType 合法取值
        validateFlowType(dto.getFlowType());

        BigDecimal amount = dto.getFlowAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流水金额必须 ≥ 0");
        }

        LocalDateTime now = LocalDateTime.now();

        // 推导同账户上一条流水的 after 作为本次 before（无历史则 0 占位）
        BigDecimal balanceBefore = findLatestBalanceAfter(dto.getAccountType(), dto.getAccountCode());
        BigDecimal balanceAfter;
        if (dto.getFlowType() == FinanceEvent.FLOW_TYPE_INCOME
                || dto.getFlowType() == FinanceEvent.FLOW_TYPE_SETTLE) {
            balanceAfter = balanceBefore.add(amount);
        } else {
            // 支出/退款：扣减
            balanceAfter = balanceBefore.subtract(amount);
        }

        FinanceFlow entity = new FinanceFlow();
        String flowCode = generateFlowCode();
        entity.setFlowCode(flowCode);
        entity.setFlowType(dto.getFlowType());
        entity.setBizType(dto.getBizType());
        entity.setBizCode(dto.getBizCode());
        entity.setAccountType(dto.getAccountType());
        entity.setAccountCode(dto.getAccountCode());
        entity.setFlowAmount(amount);
        entity.setBalanceBefore(balanceBefore);
        entity.setBalanceAfter(balanceAfter);
        entity.setPayType(dto.getPayType());
        entity.setTradeNo(dto.getTradeNo());
        entity.setCounterpartyType(dto.getCounterpartyType());
        entity.setCounterpartyCode(dto.getCounterpartyCode());
        entity.setCounterpartyName(dto.getCounterpartyName());
        entity.setFlowDescription(dto.getFlowDescription());
        entity.setFlowTime(now);
        entity.setIsSettled(0);
        entity.setStatus(FinanceEvent.FLOW_STATUS_NORMAL);
        if (balanceBefore.compareTo(BigDecimal.ZERO) == 0) {
            // 占位场景：无历史账户余额，remark 提示
            entity.setRemark(appendRemark(dto.getRemark(), "（无历史余额，before 以 0 占位）"));
        } else {
            entity.setRemark(dto.getRemark());
        }
        flowMapper.insert(entity);

        log.info("记录财务流水: flowCode={}, flowType={}, amount={}, account={}/{}",
                flowCode, dto.getFlowType(), amount, dto.getAccountType(), dto.getAccountCode());
        return flowCode;
    }

    // ====== 内部方法 ======

    @Override
    public FinanceFlow requireFlow(String flowCode) {
        FinanceFlow flow = flowMapper.selectOne(new LambdaQueryWrapper<FinanceFlow>()
                .eq(FinanceFlow::getFlowCode, flowCode)
                .last("LIMIT 1"));
        if (flow == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "财务流水不存在: " + flowCode);
        }
        return flow;
    }

    private void validateFlowType(Integer flowType) {
        if (flowType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流水类型不能为空");
        }
        if (flowType != FinanceEvent.FLOW_TYPE_INCOME
                && flowType != FinanceEvent.FLOW_TYPE_EXPENSE
                && flowType != FinanceEvent.FLOW_TYPE_REFUND
                && flowType != FinanceEvent.FLOW_TYPE_SETTLE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "流水类型非法（合法值 1=收入/2=支出/3=退款/4=结算）: " + flowType);
        }
    }

    /** 查同账户最近一条流水的 balance_after，无则返回 0 */
    private BigDecimal findLatestBalanceAfter(String accountType, String accountCode) {
        FinanceFlow latest = flowMapper.selectOne(new LambdaQueryWrapper<FinanceFlow>()
                .eq(FinanceFlow::getAccountType, accountType)
                .eq(FinanceFlow::getAccountCode, accountCode)
                .eq(FinanceFlow::getStatus, FinanceEvent.FLOW_STATUS_NORMAL)
                .orderByDesc(FinanceFlow::getFlowTime)
                .last("LIMIT 1"));
        return latest != null && latest.getBalanceAfter() != null
                ? latest.getBalanceAfter() : BigDecimal.ZERO;
    }

    private String generateFlowCode() {
        long seq = sequenceProvider.next(FinanceEvent.FLOW_SEQ_KEY);
        return FinanceEvent.FLOW_PREFIX + String.format("%0" + FinanceEvent.FLOW_SEQ_WIDTH + "d", seq);
    }

    private static String appendRemark(String origin, String suffix) {
        return (origin == null || origin.isEmpty()) ? suffix : origin + suffix;
    }

    private LambdaQueryWrapper<FinanceFlow> buildQueryWrapper(FinanceFlowQueryDTO query) {
        LambdaQueryWrapper<FinanceFlow> wrapper = new LambdaQueryWrapper<FinanceFlow>()
                .orderByDesc(FinanceFlow::getCreatedAt);
        if (query.getFlowCode() != null && !query.getFlowCode().isEmpty()) {
            wrapper.eq(FinanceFlow::getFlowCode, query.getFlowCode());
        }
        if (query.getFlowType() != null) {
            wrapper.eq(FinanceFlow::getFlowType, query.getFlowType());
        }
        if (query.getBizType() != null && !query.getBizType().isEmpty()) {
            wrapper.eq(FinanceFlow::getBizType, query.getBizType());
        }
        if (query.getBizCode() != null && !query.getBizCode().isEmpty()) {
            wrapper.eq(FinanceFlow::getBizCode, query.getBizCode());
        }
        if (query.getAccountType() != null && !query.getAccountType().isEmpty()) {
            wrapper.eq(FinanceFlow::getAccountType, query.getAccountType());
        }
        if (query.getAccountCode() != null && !query.getAccountCode().isEmpty()) {
            wrapper.eq(FinanceFlow::getAccountCode, query.getAccountCode());
        }
        if (query.getStatus() != null) {
            wrapper.eq(FinanceFlow::getStatus, query.getStatus());
        }
        if (query.getIsSettled() != null) {
            wrapper.eq(FinanceFlow::getIsSettled, query.getIsSettled());
        }
        if (query.getSettleCode() != null && !query.getSettleCode().isEmpty()) {
            wrapper.eq(FinanceFlow::getSettleCode, query.getSettleCode());
        }
        return wrapper;
    }

    private FinanceFlowVO toVO(FinanceFlow entity) {
        FinanceFlowVO vo = new FinanceFlowVO();
        vo.setId(entity.getId());
        vo.setFlowCode(entity.getFlowCode());
        vo.setFlowType(entity.getFlowType());
        vo.setBizType(entity.getBizType());
        vo.setBizCode(entity.getBizCode());
        vo.setAccountType(entity.getAccountType());
        vo.setAccountCode(entity.getAccountCode());
        vo.setFlowAmount(entity.getFlowAmount());
        vo.setBalanceBefore(entity.getBalanceBefore());
        vo.setBalanceAfter(entity.getBalanceAfter());
        vo.setPayType(entity.getPayType());
        vo.setTradeNo(entity.getTradeNo());
        vo.setCounterpartyType(entity.getCounterpartyType());
        vo.setCounterpartyCode(entity.getCounterpartyCode());
        vo.setCounterpartyName(entity.getCounterpartyName());
        vo.setFlowDescription(entity.getFlowDescription());
        vo.setFlowTime(entity.getFlowTime());
        vo.setIsSettled(entity.getIsSettled());
        vo.setSettleCode(entity.getSettleCode());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
