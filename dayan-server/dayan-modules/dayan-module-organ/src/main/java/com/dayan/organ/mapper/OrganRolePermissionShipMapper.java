package com.dayan.organ.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.organ.entity.OrganRolePermissionShip;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * organ_role_permission_ship 数据访问层。
 */
@Mapper
public interface OrganRolePermissionShipMapper extends BaseMapper<OrganRolePermissionShip> {

    /**
     * 物理删除某角色全部权限关联（授权先删后插用）。
     *
     * <p>BaseEntity 的 deleted 带 @TableLogic，delete(wrapper) 是逻辑删除，
     * 唯一键 uk_role_permission 不含 deleted，逻辑删后重插会撞键，故必须物理删。
     */
    @Delete("DELETE FROM organ_role_permission_ship WHERE role_code = #{roleCode}")
    int physicalDeleteByRoleCode(@Param("roleCode") String roleCode);
}
