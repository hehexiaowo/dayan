package com.dayan.client.service;

import com.dayan.client.dto.ClientHealthProfileSaveDTO;
import com.dayan.client.vo.ClientHealthProfileVO;

/**
 * 客户健康档案服务（一客户一档案，upsert 模式）。
 */
public interface ClientHealthProfileService {

    /**
     * 按客户编码获取健康档案。
     */
    ClientHealthProfileVO getByClient(String clientCode);

    /**
     * 保存或更新健康档案（upsert：有则更新，无则新增）。
     *
     * @return 档案主键 ID
     */
    Long saveOrUpdate(ClientHealthProfileSaveDTO dto);

    /**
     * 删除档案。
     */
    void delete(String clientCode);
}
