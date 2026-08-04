package com.dayan.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.client.entity.ClientAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * client_account 数据访问层。
 */
@Mapper
public interface ClientAccountMapper extends BaseMapper<ClientAccount> {
}
