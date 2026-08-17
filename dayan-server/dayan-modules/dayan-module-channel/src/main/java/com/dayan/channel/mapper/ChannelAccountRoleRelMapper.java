package com.dayan.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.channel.entity.ChannelAccountRoleRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * channel_account_role_rel 数据访问层。
 */
@Mapper
public interface ChannelAccountRoleRelMapper extends BaseMapper<ChannelAccountRoleRel> {

    /**
     * 物理删除该账号全部角色关联（绕过逻辑删除）。
     *
     * <p>分配角色采用"先删后增"全量覆盖：逻辑删除的 deleted=1 记录仍占用
     * uk_account_role 唯一键，重复分配同一角色会撞唯一键 → 必须物理清理。
     */
    @Delete("DELETE FROM channel_account_role_rel WHERE account_code = #{accountCode}")
    int physicallyDeleteByAccountCode(@Param("accountCode") String accountCode);
}
