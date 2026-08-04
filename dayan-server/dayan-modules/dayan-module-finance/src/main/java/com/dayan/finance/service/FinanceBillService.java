package com.dayan.finance.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.BillAuditDTO;
import com.dayan.finance.dto.BillFinishSettleDTO;
import com.dayan.finance.dto.FinanceBillQueryDTO;
import com.dayan.finance.dto.GenerateBillDTO;
import com.dayan.finance.entity.FinanceBill;
import com.dayan.finance.vo.FinanceBillVO;

import java.util.List;

/**
 * 结算单（finance_bill）服务。
 *
 * <p>查询：page/list/detail；
 * 写入：generate(0) → audit 0→1/0→4 → startSettle 1→2 → finishSettle 2→3。
 */
public interface FinanceBillService {

    PageResult<FinanceBillVO> page(FinanceBillQueryDTO query);

    List<FinanceBillVO> list(FinanceBillQueryDTO query);

    FinanceBillVO getDetail(String billCode);

    /**
     * 生成结算单：final_amount = total - commission - refund + adjust，bill_status=0。
     *
     * @return 结算单编号
     */
    String generate(GenerateBillDTO dto);

    /** 审核：0→1（通过）或 0→4（拒绝）。 */
    void audit(BillAuditDTO dto);

    /** 开始结算：1→2。 */
    void startSettle(String billCode);

    /** 完成结算：2→3 + 写 settleTime。 */
    void finishSettle(BillFinishSettleDTO dto);

    FinanceBill requireBill(String billCode);
}
