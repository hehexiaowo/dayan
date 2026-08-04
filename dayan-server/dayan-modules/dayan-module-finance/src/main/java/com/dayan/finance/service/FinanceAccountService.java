package com.dayan.finance.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.AccountReceiveDTO;
import com.dayan.finance.dto.CreateAccountDTO;
import com.dayan.finance.dto.FinanceAccountQueryDTO;
import com.dayan.finance.entity.FinanceAccount;
import com.dayan.finance.vo.FinanceAccountVO;

import java.util.List;

/**
 * 应收应付账目（finance_account）服务。
 *
 * <p>查询：page/list/detail；写入：create(0) → receive（推进 account_status：0→1 部分 / →2 结清）。
 */
public interface FinanceAccountService {

    PageResult<FinanceAccountVO> page(FinanceAccountQueryDTO query);

    List<FinanceAccountVO> list(FinanceAccountQueryDTO query);

    FinanceAccountVO getDetail(String accountCode);

    /** 创建：received=0、remain=total、account_status=0。 */
    String create(CreateAccountDTO dto);

    /**
     * 收/付款：累加 received_amount、扣减 remain_amount、推进 account_status、更新 lastReceiveTime。
     */
    void receive(AccountReceiveDTO dto);

    FinanceAccount requireAccount(String accountCode);
}
