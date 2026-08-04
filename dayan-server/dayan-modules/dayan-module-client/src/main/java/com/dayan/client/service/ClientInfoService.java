package com.dayan.client.service;

import com.dayan.client.dto.ClientInfoCreateDTO;
import com.dayan.client.dto.ClientInfoQueryDTO;
import com.dayan.client.dto.ClientInfoUpdateDTO;
import com.dayan.client.vo.ClientInfoVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 客户信息服务（按渠道隔离）。
 */
public interface ClientInfoService {

    /**
     * 客户分页列表（按 channelCode 过滤）。
     */
    PageResult<ClientInfoVO> page(ClientInfoQueryDTO query);

    /**
     * 客户详情。
     */
    ClientInfoVO getDetail(String clientCode);

    /**
     * 新增客户（client_code = CL 前缀，渠道内唯一）。
     *
     * @return clientCode
     */
    String create(ClientInfoCreateDTO dto);

    /**
     * 修改客户。
     */
    void update(String clientCode, ClientInfoUpdateDTO dto);

    /**
     * 删除客户。
     */
    void delete(String clientCode);
}
