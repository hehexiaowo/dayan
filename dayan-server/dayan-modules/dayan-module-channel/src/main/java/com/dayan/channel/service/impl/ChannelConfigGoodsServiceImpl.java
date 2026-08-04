package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.mapper.ChannelConfigGoodsMapper;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道商品配置服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelConfigGoodsServiceImpl implements ChannelConfigGoodsService {

    private final ChannelConfigGoodsMapper configGoodsMapper;

    @Override
    public List<ChannelConfigGoods> listByChannel(String channelCode) {
        requireChannelCode(channelCode);
        return configGoodsMapper.selectList(new LambdaQueryWrapper<ChannelConfigGoods>()
                .eq(ChannelConfigGoods::getChannelCode, channelCode)
                .orderByAsc(ChannelConfigGoods::getSortOrder)
                .orderByAsc(ChannelConfigGoods::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(String channelCode, List<ChannelConfigGoods> configs) {
        requireChannelCode(channelCode);
        configGoodsMapper.delete(new LambdaQueryWrapper<ChannelConfigGoods>()
                .eq(ChannelConfigGoods::getChannelCode, channelCode));
        if (configs == null || configs.isEmpty()) {
            log.info("渠道商品配置已清空: channelCode={}", channelCode);
            return;
        }
        for (ChannelConfigGoods config : configs) {
            config.setId(null);
            config.setChannelCode(channelCode);
            if (config.getStatus() == null) config.setStatus(1);
            if (config.getSortOrder() == null) config.setSortOrder(0);
            if (config.getIsExclusive() == null) config.setIsExclusive(0);
            configGoodsMapper.insert(config);
        }
        log.info("渠道商品配置批量保存成功: channelCode={}, 数量={}", channelCode, configs.size());
    }

    private void requireChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "渠道编码不能为空");
        }
    }
}
