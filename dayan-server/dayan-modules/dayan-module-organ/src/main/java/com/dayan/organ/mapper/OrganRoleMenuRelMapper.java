package com.dayan.organ.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.organ.entity.OrganRoleMenuRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * organ_role_menu_rel 数据访问层。
 */
@Mapper
public interface OrganRoleMenuRelMapper extends BaseMapper<OrganRoleMenuRel> {

    /**
     * 物理删除某角色全部菜单关联（授权先删后插用）。
     *
     * <p>BaseEntity 的 deleted 带 @TableLogic，delete(wrapper) 是逻辑删除，
     * 逻辑删后重插会撞唯一键，故必须物理删。
     */
    @Delete("DELETE FROM organ_role_menu_rel WHERE role_code = #{roleCode}")
    int physicalDeleteByRoleCode(@Param("roleCode") String roleCode);
}
