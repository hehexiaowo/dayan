package com.dayan.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.client.entity.ClientHealthProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * client_health_profile 数据访问层。
 */
@Mapper
public interface ClientHealthProfileMapper extends BaseMapper<ClientHealthProfile> {
}
