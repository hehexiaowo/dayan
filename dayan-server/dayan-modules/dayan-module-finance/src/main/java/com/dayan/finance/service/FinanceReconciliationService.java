package com.dayan.finance.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.CreateReconciliationDTO;
import com.dayan.finance.dto.FinanceReconciliationQueryDTO;
import com.dayan.finance.dto.ReconciliationConfirmDTO;
import com.dayan.finance.dto.ReconciliationSubmitDiffDTO;
import com.dayan.finance.entity.FinanceReconciliation;
import com.dayan.finance.vo.FinanceReconciliationVO;

import java.util.List;

/**
 * 对账记录（finance_reconciliation）服务。
 *
 * <p>状态机：create(0 对账中) → submitDiff 0→2（待确认） → confirm 2→3（已确认）；complete 0→1。
 */
public interface FinanceReconciliationService {

    PageResult<FinanceReconciliationVO> page(FinanceReconciliationQueryDTO query);

    List<FinanceReconciliationVO> list(FinanceReconciliationQueryDTO query);

    FinanceReconciliationVO getDetail(String reconCode);

    /** 创建：status=0（对账中） + 写 reconTime。 */
    String create(CreateReconciliationDTO dto);

    /** 完成对账（无差异）：0→1。 */
    void complete(String reconCode);

    /** 提交差异：0→2（待确认）。 */
    void submitDiff(ReconciliationSubmitDiffDTO dto);

    /** 确认：2→3（已确认）。 */
    void confirm(ReconciliationConfirmDTO dto);

    FinanceReconciliation requireReconciliation(String reconCode);
}
