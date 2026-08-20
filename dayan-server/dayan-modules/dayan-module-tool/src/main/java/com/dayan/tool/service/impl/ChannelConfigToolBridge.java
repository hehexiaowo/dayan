package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 渠道工具配置桥接：直接读 channel_config_tool 表，避免 tool -> channel 编译期循环依赖。
 *
 * <p>channel_config_tool 在 channel 域，但 tool 域的合并逻辑需要读取 config_type=1 的 repoIds。
 * 通过 JdbcTemplate 直接查询，运行时两者在同一 JVM classpath。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelConfigToolBridge {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 读取渠道补充知识库 ID 列表（从 channel_config_tool 读 config_type=1 的 config_json.repoIds）。
     *
     * @param channelCode 渠道编码
     * @param toolCode    工具编码
     * @return repoIds 列表（不存在或解析失败返回空列表）
     */
    public List<Long> listChannelRepoIds(String channelCode, String toolCode) {
        if (StrUtil.isBlank(channelCode) || StrUtil.isBlank(toolCode)) {
            return List.of();
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT config_json FROM channel_config_tool WHERE channel_code = ? AND tool_code = ? AND config_type = 1 AND deleted = 0 LIMIT 1",
                    channelCode, toolCode);
            if (rows.isEmpty()) {
                return List.of();
            }
            Object configJson = rows.get(0).get("config_json");
            if (configJson == null || StrUtil.isBlank(configJson.toString())) {
                return List.of();
            }
            JSONObject json = JSONUtil.parseObj(configJson.toString());
            JSONArray arr = json.getJSONArray("repoIds");
            if (arr == null) {
                return List.of();
            }
            return arr.toList(Long.class);
        } catch (Exception e) {
            log.warn("读取渠道补充知识库配置失败: channelCode={}, toolCode={}", channelCode, toolCode, e);
            return List.of();
        }
    }
}
