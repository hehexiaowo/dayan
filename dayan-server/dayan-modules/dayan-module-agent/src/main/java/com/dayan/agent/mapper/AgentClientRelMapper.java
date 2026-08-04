package com.dayan.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.agent.entity.AgentClientRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * agent_client_rel 数据访问层。
 */
@Mapper
public interface AgentClientRelMapper extends BaseMapper<AgentClientRel> {

    /**
     * 校验客户在当前渠道是否存在（channel_code 由租户拦截器自动追加）。
     *
     * <p>跨域只读 client_info 表，避免引入 dayan-module-client 依赖。
     */
    @Select("SELECT COUNT(1) FROM client_info WHERE client_code = #{clientCode}")
    long countClientByCode(@Param("clientCode") String clientCode);
}
