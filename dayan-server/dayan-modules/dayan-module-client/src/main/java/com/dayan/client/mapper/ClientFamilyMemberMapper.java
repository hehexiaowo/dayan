package com.dayan.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.client.entity.ClientFamilyMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * client_family_member 数据访问层。
 */
@Mapper
public interface ClientFamilyMemberMapper extends BaseMapper<ClientFamilyMember> {
}
