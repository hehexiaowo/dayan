package com.dayan.finance.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.ApplyInvoiceDTO;
import com.dayan.finance.dto.FinanceInvoiceQueryDTO;
import com.dayan.finance.dto.InvoiceAuditDTO;
import com.dayan.finance.dto.InvoiceIssueDTO;
import com.dayan.finance.dto.InvoiceOperateDTO;
import com.dayan.finance.dto.InvoiceSendDTO;
import com.dayan.finance.entity.FinanceInvoice;
import com.dayan.finance.vo.FinanceInvoiceVO;

import java.util.List;

/**
 * 发票（finance_invoice）服务。
 *
 * <p>状态机：apply(0) → audit 0→1 → issue 1→2 → send 2→3 → finish 3→4；void →5；redFlush →6。
 */
public interface FinanceInvoiceService {

    PageResult<FinanceInvoiceVO> page(FinanceInvoiceQueryDTO query);

    List<FinanceInvoiceVO> list(FinanceInvoiceQueryDTO query);

    FinanceInvoiceVO getDetail(String invoiceCode);

    /** 申请：invoice_status=0 + 写 applyTime。 */
    String apply(ApplyInvoiceDTO dto);

    /** 审核：0→1。 */
    void audit(InvoiceAuditDTO dto);

    /** 开票：1→2 + 写 invoiceNo + issueTime + invoiceUrl。 */
    void issue(InvoiceIssueDTO dto);

    /** 寄出：2→3 + 写 sendTime。 */
    void send(InvoiceSendDTO dto);

    /** 完成：3→4。 */
    void finish(InvoiceOperateDTO dto);

    /** 作废：→5。 */
    void voidInvoice(InvoiceOperateDTO dto);

    /** 红冲：→6。 */
    void redFlush(InvoiceOperateDTO dto);

    FinanceInvoice requireInvoice(String invoiceCode);
}
