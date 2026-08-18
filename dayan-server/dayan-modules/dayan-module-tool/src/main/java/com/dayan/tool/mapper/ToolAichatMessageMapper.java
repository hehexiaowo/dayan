package com.dayan.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.tool.entity.ToolAichatMessage;
import org.apache.ibatis.annotations.Mapper;

/** AI 问答消息 Mapper */
@Mapper
public interface ToolAichatMessageMapper extends BaseMapper<ToolAichatMessage> {
}
