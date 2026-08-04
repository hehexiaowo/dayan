package com.dayan.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.client.entity.ClientInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * client_info 数据访问层。
 */
@Mapper
public interface ClientInfoMapper extends BaseMapper<ClientInfo> {
}
