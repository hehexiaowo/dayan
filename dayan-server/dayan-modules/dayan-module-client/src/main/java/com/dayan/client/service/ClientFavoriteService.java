package com.dayan.client.service;

import com.dayan.client.dto.ClientFavoriteAddDTO;
import com.dayan.client.dto.ClientFavoriteQueryDTO;
import com.dayan.client.vo.ClientFavoriteVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 客户收藏服务。
 */
public interface ClientFavoriteService {

    PageResult<ClientFavoriteVO> page(ClientFavoriteQueryDTO query);

    /**
     * 添加收藏。
     *
     * @return 收藏记录主键 ID
     */
    Long add(ClientFavoriteAddDTO dto);

    /**
     * 移除收藏。
     */
    void remove(Long id);

    /**
     * 按客户编码列出收藏。
     */
    List<ClientFavoriteVO> listByClient(String clientCode);
}
