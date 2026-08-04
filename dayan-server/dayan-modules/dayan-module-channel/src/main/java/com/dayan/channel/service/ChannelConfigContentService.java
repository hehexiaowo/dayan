package com.dayan.channel.service;

import com.dayan.channel.entity.ChannelConfigContent;

import java.util.List;

/**
 * 渠道内容配置服务。
 *
 * <p>按 {@code channelCode} 维度查询与批量保存（先删后增全量覆盖）。
 */
public interface ChannelConfigContentService {

    /** 按渠道查询全部内容配置 */
    List<ChannelConfigContent> listByChannel(String channelCode);

    /**
     * 批量保存（全量覆盖）：先删该渠道下全部内容配置，再批量插入。
     */
    void saveAll(String channelCode, List<ChannelConfigContent> configs);
}
