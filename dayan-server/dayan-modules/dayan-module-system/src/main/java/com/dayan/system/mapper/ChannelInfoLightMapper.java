package com.dayan.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 渠道信息轻量查询（知识仓库树形继承解析用）。
 *
 * <p>直读 channel_info 表（平台共享表，租户拦截忽略清单内），避免 knowledge → channel
 * 模块依赖。仅取拼树所需字段，全量一次拉取（渠道数量级小），内存组装树。
 */
@Mapper
public interface ChannelInfoLightMapper {

    @Select("SELECT channel_code, full_name, short_name, parent_code, ancestors, level, sort_order " +
            "FROM channel_info WHERE deleted = 0 ORDER BY sort_order ASC, channel_code ASC")
    List<ChannelInfoLight> selectAll();
}
