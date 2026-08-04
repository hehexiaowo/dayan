package com.dayan.equity.service;

import com.dayan.equity.dto.EquityUsePersonCreateDTO;
import com.dayan.equity.dto.EquityUsePersonQueryDTO;
import com.dayan.equity.dto.EquityUsePersonUpdateDTO;
import com.dayan.equity.dto.SetDefaultHolderDTO;
import com.dayan.equity.vo.EquityUsePersonVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 权益使用人服务。
 *
 * <p>应用层校验：同 equity_code 下使用人 ≤3、身份证号唯一（解密后比对）、
 * 默认权益人唯一（设新默认时旧置 0）。
 */
public interface EquityUsePersonService {

    PageResult<EquityUsePersonVO> page(EquityUsePersonQueryDTO query);

    /** 按权益编码列出全部使用人（解密身份证后返回） */
    List<EquityUsePersonVO> listByEquity(String equityCode);

    EquityUsePersonVO getDetail(Long id);

    Long create(EquityUsePersonCreateDTO dto);

    void update(Long id, EquityUsePersonUpdateDTO dto);

    void delete(Long id);

    /** 设置默认权益人：同 equity_code 下其它使用人 is_default_holder 置 0 */
    void setDefault(SetDefaultHolderDTO dto);
}
