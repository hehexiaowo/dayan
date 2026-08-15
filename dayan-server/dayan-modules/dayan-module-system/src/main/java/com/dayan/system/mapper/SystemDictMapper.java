package com.dayan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.system.entity.SystemDict;
import org.apache.ibatis.annotations.Mapper;

/**
 * system_dict 数据访问层。
 */
@Mapper
public interface SystemDictMapper extends BaseMapper<SystemDict> {
}
