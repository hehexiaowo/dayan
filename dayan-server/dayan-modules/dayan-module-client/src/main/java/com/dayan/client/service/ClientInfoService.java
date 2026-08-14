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

    /**
     * 按渠道+手机号查找客户，不存在则自动建档（客户信息 + 无密码账号）。
     *
     * <p>用于线索留资自动转化：留资访客未必注册过客户端，此处静默建档，
     * 账号 username/password 留空（NULL），客户后续通过短信验证码登录激活。
     *
     * @param channelCode     渠道编码
     * @param phone           手机号
     * @param fullName        姓名（空则自动生成 "客户+手机号后4位"）
     * @param sourceAgentCode 来源代理人编码（可空）
     * @return clientCode
     */
    String findOrCreateByPhone(String channelCode, String phone, String fullName, String sourceAgentCode);
}
