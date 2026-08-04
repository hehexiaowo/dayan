package com.dayan.client.service;

import com.dayan.client.dto.ClientAddressCreateDTO;
import com.dayan.client.dto.ClientAddressUpdateDTO;
import com.dayan.client.vo.ClientAddressVO;

import java.util.List;

/**
 * 客户收货地址服务。
 */
public interface ClientAddressService {

    /**
     * 按客户编码列出地址。
     */
    List<ClientAddressVO> listByClient(String clientCode);

    /**
     * 新增地址（≤20 条限制校验）。
     *
     * @return 主键 ID
     */
    Long create(ClientAddressCreateDTO dto);

    /**
     * 修改地址。
     */
    void update(Long id, ClientAddressUpdateDTO dto);

    /**
     * 设为默认地址（先把同 client_code 的 is_default 全置 0，再置当前为 1）。
     */
    void setDefault(Long id);

    /**
     * 删除地址。
     */
    void delete(Long id);
}
