package com.dayan.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.agent.entity.AgentShareRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * agent_share_record 数据访问层。
 */
@Mapper
public interface AgentShareRecordMapper extends BaseMapper<AgentShareRecord> {

    /**
     * 按 clientCode 取客户姓名（跨域只读 client_info 表，同
     * {@code AgentClientRelMapper#countClientByCode} 模式，避免引入 dayan-module-client 依赖；
     * channel_code 由租户拦截器自动追加）。
     *
     * @param clientCode 客户编码
     * @return 客户姓名（client_info.full_name），客户不存在时为 null
     */
    @Select("SELECT full_name FROM client_info WHERE client_code = #{clientCode}")
    String selectClientNameByCode(@Param("clientCode") String clientCode);
}
