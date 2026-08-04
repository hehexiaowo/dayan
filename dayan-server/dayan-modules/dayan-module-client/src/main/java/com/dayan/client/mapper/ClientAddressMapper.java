package com.dayan.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.client.entity.ClientAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * client_address 数据访问层。
 */
@Mapper
public interface ClientAddressMapper extends BaseMapper<ClientAddress> {
}
