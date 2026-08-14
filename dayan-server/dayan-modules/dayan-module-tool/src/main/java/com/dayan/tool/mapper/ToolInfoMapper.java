package com.dayan.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.tool.entity.ToolInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * tool_info 数据访问层。
 */
@Mapper
public interface ToolInfoMapper extends BaseMapper<ToolInfo> {
}
