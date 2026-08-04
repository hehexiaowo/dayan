package com.dayan.channel.service;

import com.dayan.channel.entity.ChannelConfigGoods;

import java.util.List;

/**
 * 渠道商品配置服务。
 *
 * <p>按 {@code channelCode} 维度查询与批量保存（先删后增全量覆盖）。
 */
public interface ChannelConfigGoodsService {

    List<ChannelConfigGoods> listByChannel(String channelCode);

    void saveAll(String channelCode, List<ChannelConfigGoods> configs);
}
