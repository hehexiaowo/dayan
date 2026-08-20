package com.dayan.course.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 渠道课程配置桥接：直接读 channel_config_course 表，避免 course -> channel 编译期循环依赖。
 *
 * <p>channel_config_course 在 channel 域，但 course 域的 listForAgent 逻辑需要读取 config_type=0 的课程编码。
 * 通过 JdbcTemplate 直接查询，运行时两者在同一 JVM classpath。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelConfigCourseBridge {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 读取渠道已配置的课程编码列表（config_type=0, status=1）。
     *
     * @param channelCode 渠道编码
     * @return 课程编码列表（不存在返回空列表）
     */
    public List<String> listConfiguredCourseCodes(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            return List.of();
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT course_code FROM channel_config_course WHERE channel_code = ? AND config_type = 0 AND status = 1 AND deleted = 0",
                    channelCode);
            return rows.stream()
                    .map(r -> (String) r.get("course_code"))
                    .filter(StrUtil::isNotBlank)
                    .toList();
        } catch (Exception e) {
            log.warn("读取渠道课程配置失败: channelCode={}", channelCode, e);
            return List.of();
        }
    }
}
