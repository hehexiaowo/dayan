package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.entity.ChannelConfigScene;
import com.dayan.channel.mapper.ChannelConfigSceneMapper;
import com.dayan.channel.service.ChannelConfigSceneService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道场景配置服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelConfigSceneServiceImpl implements ChannelConfigSceneService {

    private final ChannelConfigSceneMapper configSceneMapper;

    @Override
    public List<ChannelConfigScene> listByChannel(String channelCode) {
        requireChannelCode(channelCode);
        return configSceneMapper.selectList(new LambdaQueryWrapper<ChannelConfigScene>()
                .eq(ChannelConfigScene::getChannelCode, channelCode)
                .orderByAsc(ChannelConfigScene::getSortOrder)
                .orderByAsc(ChannelConfigScene::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(String channelCode, List<ChannelConfigScene> configs) {
        requireChannelCode(channelCode);
        configSceneMapper.delete(new LambdaQueryWrapper<ChannelConfigScene>()
                .eq(ChannelConfigScene::getChannelCode, channelCode));
        if (configs == null || configs.isEmpty()) {
            log.info("渠道场景配置已清空: channelCode={}", channelCode);
            return;
        }
        for (ChannelConfigScene config : configs) {
            config.setId(null);
            config.setChannelCode(channelCode);
            if (config.getStatus() == null) config.setStatus(1);
            if (config.getSortOrder() == null) config.setSortOrder(0);
            if (config.getIsExclusive() == null) config.setIsExclusive(0);
            configSceneMapper.insert(config);
        }
        log.info("渠道场景配置批量保存成功: channelCode={}, 数量={}", channelCode, configs.size());
    }

    private void requireChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "渠道编码不能为空");
        }
    }
}
