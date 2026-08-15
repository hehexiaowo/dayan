package com.dayan.goods.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务网络范围（service_item.service_network / goods_service_item_rel.network_scope JSON
 * 的结构化形态）。
 *
 * <p>NULL（无 JSON）= 该服务项目所属业态（旅游短居/活力长居/照护长居，对应
 * park_info.network_tags）的全部在营机构；mode=custom 时为自选范围，可精确到
 * 机构的具体房型（随心住类：如"任选其中一家机构的一间房型免费入住"）。
 *
 * <p>层级：服务项目级定义默认可用网络（含房型精度）；商品级（rel.network_scope）
 * 可进一步收窄，NULL=继承服务项目的范围。
 */
@Data
public class NetworkScope {

    /** custom=自选机构范围（all 或空视为业态全部，与 NULL 等价） */
    private String mode;

    /** 自选机构列表（mode=custom 时有效） */
    private List<ParkScope> parks;

    public boolean isCustom() {
        return "custom".equalsIgnoreCase(mode) && parks != null && !parks.isEmpty();
    }

    /**
     * 单个机构的范围：勾选机构=整馆（roomTypeCodes 空）；只勾部分房型=具体编码列表。
     */
    @Data
    public static class ParkScope {

        /** 机构编码 */
        private String parkCode;

        /** 房型编码列表（空/null=该机构全部房型） */
        private List<String> roomTypeCodes = new ArrayList<>();

        public boolean wholePark() {
            return roomTypeCodes == null || roomTypeCodes.isEmpty();
        }
    }
}
