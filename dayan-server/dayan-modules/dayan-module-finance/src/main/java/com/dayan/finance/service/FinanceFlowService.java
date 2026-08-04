package com.dayan.finance.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.finance.dto.FinanceFlowQueryDTO;
import com.dayan.finance.dto.RecordFlowDTO;
import com.dayan.finance.entity.FinanceFlow;
import com.dayan.finance.vo.FinanceFlowVO;

import java.util.List;

/**
 * 财务流水（finance_flow）服务。
 *
 * <p>查询：page/list/detail；写入：record（生成 FL+序号 + flowTime=now）。
 */
public interface FinanceFlowService {

    PageResult<FinanceFlowVO> page(FinanceFlowQueryDTO query);

    List<FinanceFlowVO> list(FinanceFlowQueryDTO query);

    FinanceFlowVO getDetail(String flowCode);

    /**
     * 记录一条财务流水。
     *
     * <p>生成流水编号 FL+序号；flowTime=now；balance_before/after 由同账户最近一条 after 推导，无历史则 0 占位。
     *
     * @return 流水编号
     */
    String record(RecordFlowDTO dto);

    /**
     * 内部辅助：按编码查实体（不存在抛 NOT_FOUND）。
     */
    FinanceFlow requireFlow(String flowCode);
}
