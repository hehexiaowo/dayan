package com.dayan.butler.service;

import com.dayan.butler.dto.ButlerClientRelBindDTO;
import com.dayan.butler.dto.ButlerClientRelQueryDTO;
import com.dayan.butler.vo.ButlerClientRelVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 管家-客户绑定关系服务。
 *
 * <p><b>一客户一管家约束</b>：同一 clientCode 仅允许 1 条 status=1 的有效绑定。
 * 绑定时若已有有效绑定则拒绝（需先解绑），解绑=将 status 置 0。
 */
public interface ButlerClientRelService {

    PageResult<ButlerClientRelVO> page(ButlerClientRelQueryDTO query);

    List<ButlerClientRelVO> list(ButlerClientRelQueryDTO query);

    ButlerClientRelVO getDetail(Long id);

    /** 绑定管家与客户（一客户一管家约束） */
    Long bind(ButlerClientRelBindDTO dto);

    /** 解绑：将 status 置 0 */
    void unbind(Long id);

    void delete(Long id);
}
