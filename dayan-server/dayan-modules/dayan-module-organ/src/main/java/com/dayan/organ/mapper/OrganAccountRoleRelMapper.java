package com.dayan.organ.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.organ.entity.OrganAccountRoleRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * organ_account_role_rel 数据访问层。
 */
@Mapper
public interface OrganAccountRoleRelMapper extends BaseMapper<OrganAccountRoleRel> {

    /**
     * 物理删除该账号全部角色关联（绕过逻辑删除）。
     *
     * <p>分配角色采用"先删后增"全量覆盖：逻辑删除的 deleted=1 记录仍占用
     * uk_account_role 唯一键，重复分配同一角色会撞唯一键 → 必须物理清理。
     */
    @Delete("DELETE FROM organ_account_role_rel WHERE account_code = #{accountCode}")
    int physicallyDeleteByAccountCode(@Param("accountCode") String accountCode);
}
