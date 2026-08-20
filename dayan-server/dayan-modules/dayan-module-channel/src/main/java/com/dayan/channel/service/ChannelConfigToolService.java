package com.dayan.channel.service;

import com.dayan.channel.entity.ChannelConfigTool;

import java.util.List;

/**
 * 渠道工具配置服务（通用读写：按 channel_code + tool_code + config_type 操作 config_json）。
 *
 * <p>对齐 channel_config_content/scene/goods 的先删后增全量覆盖模式。</p>
 */
public interface ChannelConfigToolService {

    /**
     * 按 channel + tool + type 读配置（不存在返回 null）。
     */
    ChannelConfigTool getByChannelToolType(String channelCode, String toolCode, int configType);

    /**
     * 保存/更新配置（全量替换：删除旧行后插入；configJson 必须为合法 JSON）。
     */
    void save(String channelCode, String toolCode, int configType, String configJson);

    /**
     * 按渠道查询所有已配置的工具编码（config_type 指定，status=1）。
     */
    List<String> listConfiguredToolCodes(String channelCode, int configType);

    /**
     * 按渠道查询所有配置（用于 admin 端列表）。
     */
    List<ChannelConfigTool> listByChannel(String channelCode);

    /**
     * 批量保存（全量覆盖：先删后增）。
     */
    void saveAll(String channelCode, List<ChannelConfigTool> configs);
}
