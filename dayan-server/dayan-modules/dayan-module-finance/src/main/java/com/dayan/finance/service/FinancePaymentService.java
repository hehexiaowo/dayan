package com.dayan.finance.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.CreatePaymentDTO;
import com.dayan.finance.dto.FinancePaymentQueryDTO;
import com.dayan.finance.dto.PaymentMarkFailedDTO;
import com.dayan.finance.dto.PaymentMarkSuccessDTO;
import com.dayan.finance.entity.FinancePayment;
import com.dayan.finance.vo.FinancePaymentVO;

import java.util.List;

/**
 * 订单支付记录（finance_payment）服务。
 *
 * <p>状态机：create(0 待支付) → markSuccess 0→1 / markFailed 0→2。
 */
public interface FinancePaymentService {

    PageResult<FinancePaymentVO> page(FinancePaymentQueryDTO query);

    List<FinancePaymentVO> list(FinancePaymentQueryDTO query);

    FinancePaymentVO getDetail(String paymentCode);

    /** 创建：生成 PAY+序号，pay_status=0（待支付）。 */
    String create(CreatePaymentDTO dto);

    /** 标记成功：0→1 + 写 tradeNo/payTime/notifyTime。 */
    void markSuccess(PaymentMarkSuccessDTO dto);

    /** 标记失败：0→2。 */
    void markFailed(PaymentMarkFailedDTO dto);

    FinancePayment requirePayment(String paymentCode);
}
