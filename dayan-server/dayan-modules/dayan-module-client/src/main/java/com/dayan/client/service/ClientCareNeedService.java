package com.dayan.client.service;

import com.dayan.client.dto.ClientCareNeedCreateDTO;
import com.dayan.client.dto.ClientCareNeedQueryDTO;
import com.dayan.client.dto.ClientCareNeedUpdateDTO;
import com.dayan.client.vo.ClientCareNeedVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 客户照护需求评估服务。
 */
public interface ClientCareNeedService {

    PageResult<ClientCareNeedVO> page(ClientCareNeedQueryDTO query);

    /**
     * 按客户编码列出评估列表。
     */
    List<ClientCareNeedVO> listByClient(String clientCode);

    /**
     * 新增评估。
     *
     * @return 评估记录主键 ID
     */
    Long create(ClientCareNeedCreateDTO dto);

    /**
     * 修改评估。
     */
    void update(Long id, ClientCareNeedUpdateDTO dto);

    /**
     * 删除评估。
     */
    void delete(Long id);
}
