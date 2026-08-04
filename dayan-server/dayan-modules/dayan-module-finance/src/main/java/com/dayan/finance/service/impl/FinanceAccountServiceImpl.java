package com.dayan.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.AccountReceiveDTO;
import com.dayan.finance.dto.CreateAccountDTO;
import com.dayan.finance.dto.FinanceAccountQueryDTO;
import com.dayan.finance.entity.FinanceAccount;
import com.dayan.finance.enums.FinanceEvent;
import com.dayan.finance.mapper.FinanceAccountMapper;
import com.dayan.finance.service.FinanceAccountService;
import com.dayan.finance.vo.FinanceAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应收应付账目（finance_account）服务实现。
 *
 * <p>状态推进（receive 时按 remain_amount 推导）：
 * <ul>
 *   <li>remain > 0：0→1（部分收/付）</li>
 *   <li>remain ≤ 0：→2（已结清）</li>
 * </ul>
 * 编码：ACC + 10 位序号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceAccountServiceImpl implements FinanceAccountService {

    private final FinanceAccountMapper accountMapper;
    private final SequenceProvider sequenceProvider;

    // ====== 查询 ======

    @Override
    public PageResult<FinanceAccountVO> page(FinanceAccountQueryDTO query) {
        LambdaQueryWrapper<FinanceAccount> wrapper = buildQueryWrapper(query);
        Page<FinanceAccount> page = accountMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FinanceAccountVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<FinanceAccountVO> list(FinanceAccountQueryDTO query) {
        return accountMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public FinanceAccountVO getDetail(String accountCode) {
        return toVO(requireAccount(accountCode));
    }

    // ====== 写入 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CreateAccountDTO dto) {
        if (dto.getDirection() == null
                || (dto.getDirection() != FinanceEvent.DIRECTION_RECEIVABLE
                && dto.getDirection() != FinanceEvent.DIRECTION_PAYABLE)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账目方向必须为 1(应收) 或 2(应付)");
        }
        if (dto.getTotalAmount() == null || dto.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "总额必须 > 0");
        }

        FinanceAccount entity = new FinanceAccount();
        String accountCode = generateAccountCode();
        entity.setAccountCode(accountCode);
        entity.setDirection(dto.getDirection());
        entity.setAccountType(dto.getAccountType());
        entity.setTargetCode(dto.getTargetCode());
        entity.setTargetName(dto.getTargetName());
        entity.setBizType(dto.getBizType());
        entity.setBizCode(dto.getBizCode());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setReceivedAmount(BigDecimal.ZERO);
        entity.setRemainAmount(dto.getTotalAmount());
        entity.setDueDate(dto.getDueDate());
        entity.setAccountStatus(FinanceEvent.ACCOUNT_STATUS_PENDING);
        entity.setRemark(dto.getRemark());
        accountMapper.insert(entity);

        log.info("创建账目: accountCode={}, direction={}, total={}",
                accountCode, dto.getDirection(), dto.getTotalAmount());
        return accountCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(AccountReceiveDTO dto) {
        FinanceAccount account = requireAccount(dto.getAccountCode());
        BigDecimal amount = dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收/付金额必须 > 0");
        }
        int from = account.getAccountStatus() == null
                ? FinanceEvent.ACCOUNT_STATUS_PENDING : account.getAccountStatus();
        // 只允许 待收付(0)/部分(1) 继续收款；已结清/逾期/坏账 不允许
        if (from != FinanceEvent.ACCOUNT_STATUS_PENDING
                && from != FinanceEvent.ACCOUNT_STATUS_PARTIAL) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "账目状态不允许收/付款（当前状态=" + from + "）");
        }

        BigDecimal receivedOld = account.getReceivedAmount() == null
                ? BigDecimal.ZERO : account.getReceivedAmount();
        BigDecimal remainOld = account.getRemainAmount() == null
                ? BigDecimal.ZERO : account.getRemainAmount();
        BigDecimal receivedNew = receivedOld.add(amount);
        BigDecimal remainNew = remainOld.subtract(amount);

        int to = (remainNew.compareTo(BigDecimal.ZERO) <= 0)
                ? FinanceEvent.ACCOUNT_STATUS_SETTLED
                : FinanceEvent.ACCOUNT_STATUS_PARTIAL;

        LocalDateTime now = LocalDateTime.now();
        FinanceAccount update = new FinanceAccount();
        update.setId(account.getId());
        update.setReceivedAmount(receivedNew);
        update.setRemainAmount(remainNew);
        update.setAccountStatus(to);
        update.setLastReceiveTime(dto.getReceiveTime() != null ? dto.getReceiveTime() : now);
        if (dto.getRemark() != null && !dto.getRemark().isEmpty()) {
            update.setRemark(dto.getRemark());
        }
        accountMapper.updateById(update);
        log.info("账目收/付款: accountCode={}, amount={}, received={}, remain={}, status={}",
                dto.getAccountCode(), amount, receivedNew, remainNew, to);
    }

    // ====== 内部方法 ======

    @Override
    public FinanceAccount requireAccount(String accountCode) {
        FinanceAccount account = accountMapper.selectOne(new LambdaQueryWrapper<FinanceAccount>()
                .eq(FinanceAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账目不存在: " + accountCode);
        }
        return account;
    }

    private String generateAccountCode() {
        long seq = sequenceProvider.next(FinanceEvent.ACCOUNT_SEQ_KEY);
        return FinanceEvent.ACCOUNT_PREFIX + String.format("%0" + FinanceEvent.ACCOUNT_SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<FinanceAccount> buildQueryWrapper(FinanceAccountQueryDTO query) {
        LambdaQueryWrapper<FinanceAccount> wrapper = new LambdaQueryWrapper<FinanceAccount>()
                .orderByDesc(FinanceAccount::getCreatedAt);
        if (query.getAccountCode() != null && !query.getAccountCode().isEmpty()) {
            wrapper.eq(FinanceAccount::getAccountCode, query.getAccountCode());
        }
        if (query.getDirection() != null) {
            wrapper.eq(FinanceAccount::getDirection, query.getDirection());
        }
        if (query.getAccountType() != null && !query.getAccountType().isEmpty()) {
            wrapper.eq(FinanceAccount::getAccountType, query.getAccountType());
        }
        if (query.getTargetCode() != null && !query.getTargetCode().isEmpty()) {
            wrapper.eq(FinanceAccount::getTargetCode, query.getTargetCode());
        }
        if (query.getBizType() != null && !query.getBizType().isEmpty()) {
            wrapper.eq(FinanceAccount::getBizType, query.getBizType());
        }
        if (query.getBizCode() != null && !query.getBizCode().isEmpty()) {
            wrapper.eq(FinanceAccount::getBizCode, query.getBizCode());
        }
        if (query.getAccountStatus() != null) {
            wrapper.eq(FinanceAccount::getAccountStatus, query.getAccountStatus());
        }
        if (query.getDueDateTo() != null) {
            wrapper.le(FinanceAccount::getDueDate, query.getDueDateTo());
        }
        return wrapper;
    }

    private FinanceAccountVO toVO(FinanceAccount entity) {
        FinanceAccountVO vo = new FinanceAccountVO();
        vo.setId(entity.getId());
        vo.setAccountCode(entity.getAccountCode());
        vo.setDirection(entity.getDirection());
        vo.setAccountType(entity.getAccountType());
        vo.setTargetCode(entity.getTargetCode());
        vo.setTargetName(entity.getTargetName());
        vo.setBizType(entity.getBizType());
        vo.setBizCode(entity.getBizCode());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setReceivedAmount(entity.getReceivedAmount());
        vo.setRemainAmount(entity.getRemainAmount());
        vo.setDueDate(entity.getDueDate());
        vo.setLastReceiveTime(entity.getLastReceiveTime());
        vo.setAccountStatus(entity.getAccountStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
