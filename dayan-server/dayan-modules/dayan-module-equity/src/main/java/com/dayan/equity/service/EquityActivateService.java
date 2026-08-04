package com.dayan.equity.service;

import com.dayan.equity.dto.EquityActivateQueryDTO;
import com.dayan.equity.vo.EquityActivateVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 权益激活记录服务（仅查询，由 depot.activate 自动产生）。
 */
public interface EquityActivateService {

    PageResult<EquityActivateVO> page(EquityActivateQueryDTO query);

    List<EquityActivateVO> list(EquityActivateQueryDTO query);

    /** 按权益编码查激活记录（一权益一记录） */
    EquityActivateVO getByEquityCode(String equityCode);
}
