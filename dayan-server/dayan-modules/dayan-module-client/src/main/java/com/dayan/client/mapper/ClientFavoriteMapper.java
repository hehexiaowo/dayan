package com.dayan.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.client.entity.ClientFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * client_favorite 数据访问层。
 */
@Mapper
public interface ClientFavoriteMapper extends BaseMapper<ClientFavorite> {
}
