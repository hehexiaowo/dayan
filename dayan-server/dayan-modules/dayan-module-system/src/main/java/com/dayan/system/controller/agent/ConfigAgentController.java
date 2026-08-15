package com.dayan.system.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Agent 端公开配置下发。
 *
 * <p>GET /agent-api/v1/config/map-key：下发天地图前端 Key。天地图 Key 为
 * 前端暴露型（随瓦片 URL 明文出现，官方设计如此），端点匿名可用；
 * 值收口在系统配置表（map 组 map.tianditu-key），管理员改配置即全端生效，
 * 前端不再硬编码。
 */
@Tag(name = "Agent 端-公开配置")
@RestController
@RequestMapping("/v1/config")
@RequiredArgsConstructor
@Slf4j
public class ConfigAgentController {

    /** 兜底链最后一环：DB 无值且未配置 dayan.map.tianditu-key 时使用（离线开发不断图） */
    private static final String FALLBACK_TIANDITU_KEY = "1ea38bada071978da6b6cfd68c464450";

    private final SystemConfigService configService;

    @Value("${dayan.map.tianditu-key:}")
    private String configuredTiandituKey;

    @Operation(summary = "天地图前端 Key（匿名公开：前端暴露型 Key）")
    @GetMapping("/map-key")
    public R<Map<String, String>> mapKey() {
        String key = configService.getValue("map", "map.tianditu-key");
        if (key == null || key.isBlank()) {
            key = configuredTiandituKey;
        }
        if (key == null || key.isBlank()) {
            key = FALLBACK_TIANDITU_KEY;
        }
        return R.ok(Map.of("tiandituKey", key));
    }
}
