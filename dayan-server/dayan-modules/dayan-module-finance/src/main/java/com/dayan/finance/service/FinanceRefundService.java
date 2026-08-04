package com.dayan.finance.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.ApplyRefundDTO;
import com.dayan.finance.dto.FinanceRefundQueryDTO;
import com.dayan.finance.dto.RefundAuditDTO;
import com.dayan.finance.dto.RefundMarkFailedDTO;
import com.dayan.finance.dto.RefundMarkSuccessDTO;
import com.dayan.finance.entity.FinanceRefund;
import com.dayan.finance.vo.FinanceRefundVO;

import java.util.List;

/**
 * 订单退款记录（finance_refund）服务。
 *
 * <p>状态机：apply(0 待审核) → audit 0→1/0→4 → markRefunding 1→2 → markSuccess 2→3 / markFailed 2→5。
 */
public interface FinanceRefundService {

    PageResult<FinanceRefundVO> page(FinanceRefundQueryDTO query);

    List<FinanceRefundVO> list(FinanceRefundQueryDTO query);

    FinanceRefundVO getDetail(String refundCode);

    /** 申请：生成 RF+序号，refund_status=0 + 写 applyTime。 */
    String apply(ApplyRefundDTO dto);

    /** 审核：0→1（通过）或 0→4（拒绝）。 */
    void audit(RefundAuditDTO dto);

    /** 进入退款中：1→2。 */
    void markRefunding(String refundCode);

    /** 标记退款成功：2→3 + 写 refundTradeNo + refundTime。 */
    void markSuccess(RefundMarkSuccessDTO dto);

    /** 标记退款失败：2→5。 */
    void markFailed(RefundMarkFailedDTO dto);

    FinanceRefund requireRefund(String refundCode);
}
