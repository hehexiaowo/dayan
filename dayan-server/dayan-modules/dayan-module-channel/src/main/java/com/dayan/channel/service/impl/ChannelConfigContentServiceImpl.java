package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.entity.ChannelConfigContent;
import com.dayan.channel.mapper.ChannelConfigContentMapper;
import com.dayan.channel.service.ChannelConfigContentService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道内容配置服务实现。
 *
 * <p>批量保存采用"先删后增"全量覆盖语义，channelCode 由方法参数强制覆盖（防止前端串号）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelConfigContentServiceImpl implements ChannelConfigContentService {

    private final ChannelConfigContentMapper configContentMapper;

    @Override
    public List<ChannelConfigContent> listByChannel(String channelCode) {
        requireChannelCode(channelCode);
        return configContentMapper.selectList(new LambdaQueryWrapper<ChannelConfigContent>()
                .eq(ChannelConfigContent::getChannelCode, channelCode)
                .orderByAsc(ChannelConfigContent::getSortOrder)
                .orderByAsc(ChannelConfigContent::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(String channelCode, List<ChannelConfigContent> configs) {
        requireChannelCode(channelCode);
        // 先删后增（全量覆盖）
        configContentMapper.delete(new LambdaQueryWrapper<ChannelConfigContent>()
                .eq(ChannelConfigContent::getChannelCode, channelCode));
        if (configs == null || configs.isEmpty()) {
            log.info("渠道内容配置已清空: channelCode={}", channelCode);
            return;
        }
        for (ChannelConfigContent config : configs) {
            // 强制以方法参数的 channelCode 为准，忽略入参中的值
            config.setId(null);
            config.setChannelCode(channelCode);
            if (config.getStatus() == null) config.setStatus(1);
            if (config.getSortOrder() == null) config.setSortOrder(0);
            if (config.getIsTop() == null) config.setIsTop(0);
            configContentMapper.insert(config);
        }
        log.info("渠道内容配置批量保存成功: channelCode={}, 数量={}", channelCode, configs.size());
    }

    private void requireChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "渠道编码不能为空");
        }
    }
}
