package com.dayan.channel.service;

import com.dayan.channel.entity.ChannelConfigCourse;
import java.util.List;

/**
 * 渠道课程配置服务。
 */
public interface ChannelConfigCourseService {

    /**
     * 按渠道+课程+类型读配置（不存在返回 null）。
     */
    ChannelConfigCourse getByChannelCourseType(String channelCode, String courseCode, int configType);

    /**
     * 保存/更新配置（全量替换：删除旧行后插入；configJson 必须为合法 JSON）。
     */
    void save(String channelCode, String courseCode, int configType, String configJson);

    /**
     * 按渠道查询所有已配置的课程编码（config_type=0，status=1）。
     */
    List<String> listConfiguredCourseCodes(String channelCode);

    /**
     * 按渠道查询所有配置（用于 admin 端列表）。
     */
    List<ChannelConfigCourse> listByChannel(String channelCode);

    /**
     * 批量保存（全量覆盖：先删后增）。
     */
    void saveAll(String channelCode, List<ChannelConfigCourse> configs);
}
