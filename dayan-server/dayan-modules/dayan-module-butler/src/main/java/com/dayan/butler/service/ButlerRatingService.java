package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerRatingCreateDTO;
import com.dayan.butler.dto.ButlerRatingQueryDTO;
import com.dayan.butler.dto.ButlerRatingUpdateDTO;
import com.dayan.butler.vo.ButlerRatingVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家评价服务。
 *
 * <p>按 butlerCode/clientCode 查询，rating 取值 1-5。
 */
public interface ButlerRatingService {

    PageResult<ButlerRatingVO> page(ButlerRatingQueryDTO query);

    List<ButlerRatingVO> list(ButlerRatingQueryDTO query);

    ButlerRatingVO getDetail(Long id);

    Long create(ButlerRatingCreateDTO dto);

    void update(Long id, ButlerRatingUpdateDTO dto);

    void delete(Long id);
}
