package com.dayan.channel.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.entity.ChannelConfigCourse;
import com.dayan.channel.mapper.ChannelConfigCourseMapper;
import com.dayan.channel.service.ChannelConfigCourseService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 渠道课程配置服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelConfigCourseServiceImpl implements ChannelConfigCourseService {

    private final ChannelConfigCourseMapper configCourseMapper;

    @Override
    public ChannelConfigCourse getByChannelCourseType(String channelCode, String courseCode, int configType) {
        requireNotBlank(channelCode, "渠道编码不能为空");
        requireNotBlank(courseCode, "课程编码不能为空");
        return configCourseMapper.selectOne(new LambdaQueryWrapper<ChannelConfigCourse>()
                .eq(ChannelConfigCourse::getChannelCode, channelCode)
                .eq(ChannelConfigCourse::getCourseCode, courseCode)
                .eq(ChannelConfigCourse::getConfigType, configType)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String channelCode, String courseCode, int configType, String configJson) {
        requireNotBlank(channelCode, "渠道编码不能为空");
        requireNotBlank(courseCode, "课程编码不能为空");
        validateJson(configJson);

        // 全量替换：先删旧行再插入
        configCourseMapper.delete(new LambdaQueryWrapper<ChannelConfigCourse>()
                .eq(ChannelConfigCourse::getChannelCode, channelCode)
                .eq(ChannelConfigCourse::getCourseCode, courseCode)
                .eq(ChannelConfigCourse::getConfigType, configType));

        ChannelConfigCourse entity = new ChannelConfigCourse();
        entity.setChannelCode(channelCode);
        entity.setCourseCode(courseCode);
        entity.setConfigType(configType);
        entity.setConfigJson(configJson == null ? "{}" : configJson);
        entity.setStatus(1);
        configCourseMapper.insert(entity);

        log.info("渠道课程配置保存成功: channelCode={}, courseCode={}, configType={}", channelCode, courseCode, configType);
    }

    @Override
    public List<String> listConfiguredCourseCodes(String channelCode) {
        requireNotBlank(channelCode, "渠道编码不能为空");
        List<ChannelConfigCourse> list = configCourseMapper.selectList(new LambdaQueryWrapper<ChannelConfigCourse>()
                .select(ChannelConfigCourse::getCourseCode)
                .eq(ChannelConfigCourse::getChannelCode, channelCode)
                .eq(ChannelConfigCourse::getConfigType, 0)
                .eq(ChannelConfigCourse::getStatus, 1));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(ChannelConfigCourse::getCourseCode)
                .collect(Collectors.toList());
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
