package com.dayan.organ.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.organ.dto.OrganInfoCreateDTO;
import com.dayan.organ.dto.OrganInfoQueryDTO;
import com.dayan.organ.dto.OrganInfoUpdateDTO;
import com.dayan.organ.vo.OrganInfoSimpleVO;
import com.dayan.organ.vo.OrganInfoVO;

/**
 * 组织信息（公司/分公司）服务。
 */
public interface OrganInfoService {

    PageResult<OrganInfoVO> page(OrganInfoQueryDTO query);

    OrganInfoVO getDetail(String organCode);

    String create(OrganInfoCreateDTO dto);

    void update(String organCode, OrganInfoUpdateDTO dto);

    void delete(String organCode);

    /**
     * 全量启用组织列表（下拉选择用，不分页）。
     */
    java.util.List<OrganInfoSimpleVO> listAll();
}
