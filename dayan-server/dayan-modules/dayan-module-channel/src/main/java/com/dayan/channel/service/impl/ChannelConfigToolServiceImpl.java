package com.dayan.channel.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.entity.ChannelConfigTool;
import com.dayan.channel.mapper.ChannelConfigToolMapper;
import com.dayan.channel.service.ChannelConfigToolService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 渠道工具配置服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelConfigToolServiceImpl implements ChannelConfigToolService {

    private final ChannelConfigToolMapper configToolMapper;

    @Override
    public ChannelConfigTool getByChannelToolType(String channelCode, String toolCode, int configType) {
        requireNotBlank(channelCode, "渠道编码不能为空");
        requireNotBlank(toolCode, "工具编码不能为空");
        return configToolMapper.selectOne(new LambdaQueryWrapper<ChannelConfigTool>()
                .eq(ChannelConfigTool::getChannelCode, channelCode)
                .eq(ChannelConfigTool::getToolCode, toolCode)
                .eq(ChannelConfigTool::getConfigType, configType)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String channelCode, String toolCode, int configType, String configJson) {
        requireNotBlank(channelCode, "渠道编码不能为空");
        requireNotBlank(toolCode, "工具编码不能为空");
        validateJson(configJson);

        // 全量替换：先删旧行再插入
        configToolMapper.delete(new LambdaQueryWrapper<ChannelConfigTool>()
                .eq(ChannelConfigTool::getChannelCode, channelCode)
                .eq(ChannelConfigTool::getToolCode, toolCode)
                .eq(ChannelConfigTool::getConfigType, configType));

        ChannelConfigTool entity = new ChannelConfigTool();
        entity.setChannelCode(channelCode);
        entity.setToolCode(toolCode);
        entity.setConfigType(configType);
        entity.setConfigJson(configJson == null ? "{}" : configJson);
        configToolMapper.insert(entity);

        log.info("渠道工具配置保存成功: channelCode={}, toolCode={}, configType={}", channelCode, toolCode, configType);
    }

    private void requireNotBlank(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private void validateJson(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return;
        }
        try {
            JSONUtil.parse(configJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "配置内容不是合法 JSON");
        }
    }
}
