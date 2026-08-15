package com.dayan.common.core.enums;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;

import java.util.LinkedHashSet;

/**
 * 养老业态（三业态）：与 system_dict_business(dict_type='network_type') 字典一一对应。
 *
 * <p>存储形态：逗号分隔字符串（如 "vital,care"）；NULL/空 = 全部业态。
 * 用于 park_info.network_tags、park_display_block.network_tags、content_info.network_tags。
 */
public enum NetworkType {

    VITAL("vital", "活力长居"),
    CARE("care", "照护长居"),
    SOJOURN("sojourn", "旅游短居");

    private final String code;
    private final String label;

    NetworkType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    /** 按 code 精确解析（大小写敏感），非法返回 null */
    public static NetworkType of(String code) {
        for (NetworkType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 校验并规范化逗号分隔业态串：去空白、去重、保序。
     * 空串返回 null（=全部业态）；任一非法值抛 BusinessException。
     */
    public static String normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return null;
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String part : tags.split(",")) {
            String v = part.trim();
            if (v.isEmpty()) {
                continue;
            }
            if (of(v) == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "非法业态值: " + v + "（合法值: vital/care/sojourn）");
            }
            set.add(v);
        }
        return set.isEmpty() ? null : String.join(",", set);
    }
}
