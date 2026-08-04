package com.dayan.equity.service;

import com.dayan.equity.dto.EquityChangeHolderQueryDTO;
import com.dayan.equity.vo.EquityChangeHolderVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 权益更换权益人记录服务（仅查询；发起/完成/回滚操作由 {@code EquityDepotService} 提供）。
 */
public interface EquityChangeHolderService {

    PageResult<EquityChangeHolderVO> page(EquityChangeHolderQueryDTO query);

    /** 按权益编码列出更换历史 */
    List<EquityChangeHolderVO> listByEquity(String equityCode);

    EquityChangeHolderVO getDetail(Long id);
}
