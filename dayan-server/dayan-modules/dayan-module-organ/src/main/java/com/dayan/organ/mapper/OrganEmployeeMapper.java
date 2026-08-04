package com.dayan.organ.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.organ.entity.OrganEmployee;
import org.apache.ibatis.annotations.Mapper;

/**
 * organ_employee 数据访问层。
 */
@Mapper
public interface OrganEmployeeMapper extends BaseMapper<OrganEmployee> {
}
