package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerInfoCreateDTO;
import com.dayan.butler.dto.ButlerInfoQueryDTO;
import com.dayan.butler.dto.ButlerInfoUpdateDTO;
import com.dayan.butler.vo.ButlerInfoVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家信息服务。
 *
 * <p>{@code butlerCode} 由系统生成（BT + 5 位序列），全表唯一。
 */
public interface ButlerInfoService {

    PageResult<ButlerInfoVO> page(ButlerInfoQueryDTO query);

    List<ButlerInfoVO> list(ButlerInfoQueryDTO query);

    ButlerInfoVO getDetail(String butlerCode);

    String create(ButlerInfoCreateDTO dto);

    void update(String butlerCode, ButlerInfoUpdateDTO dto);

    void delete(String butlerCode);
}
