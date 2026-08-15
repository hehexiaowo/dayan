package com.dayan.goods.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 权益内容 JSON 字段（holder_rule / network_scope / usage_rule）的序列化工具。
 *
 * <p>实体侧存 TEXT JSON，DTO/VO 侧用结构化对象，本类负责双向转换。
 * 反序列化忽略未知字段（向前兼容）；失败返回 null 并告警，不阻断主流程。
 */
@Slf4j
public final class RightsJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private RightsJson() {
    }

    /** 对象 → JSON 字符串；null 对象返回 null */
    public static String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("权益内容JSON序列化失败: " + e.getMessage(), e);
        }
    }

    /** JSON 字符串 → 对象；空串/失败返回 null（告警不抛错，兼容脏数据） */
    public static <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            log.warn("权益内容JSON解析失败（按空处理）: type={}, err={}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 服务网络范围解析（兼容旧格式）。
     *
     * <p>旧 service_item.service_network 存的是字符串数组（通配符/机构码），如
     * {@code ["*"]}、{@code ["旅居*"]}、{@code ["PARK001"]}。本方法：
     * <ul>
     *   <li>对象格式（NetworkScope）→ 直接解析</li>
     *   <li>数组含 "*" 或 "*"-结尾通配 → 返回 null（=业态全部机构）</li>
     *   <li>数组为具体机构码 → 转为 custom + 整馆机构列表</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public static NetworkScope readNetwork(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> codes = MAPPER.readValue(trimmed, List.class);
                boolean hasWildcard = codes == null || codes.isEmpty()
                        || codes.stream().anyMatch(c -> c == null || c.contains("*"));
                if (hasWildcard) {
                    return null;
                }
                NetworkScope scope = new NetworkScope();
                scope.setMode("custom");
                List<NetworkScope.ParkScope> parks = new ArrayList<>();
                for (String code : codes) {
                    NetworkScope.ParkScope p = new NetworkScope.ParkScope();
                    p.setParkCode(code);
                    parks.add(p);
                }
                scope.setParks(parks);
                return scope;
            } catch (Exception e) {
                log.warn("服务网络旧格式解析失败（按全部机构处理）: {}", e.getMessage());
                return null;
            }
        }
        return read(trimmed, NetworkScope.class);
    }
}
