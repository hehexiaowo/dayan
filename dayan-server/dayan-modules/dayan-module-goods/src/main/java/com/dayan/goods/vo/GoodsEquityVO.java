package com.dayan.goods.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权益商品配置 VO（含 1:1 配置 + 关联的服务项目列表）。
 */
@Data
public class GoodsEquityVO {

    private Long id;
    private String goodsCode;
    private Integer personCount;
    private Integer validDays;
    private Integer shelfLifeDays;
    private Integer maxTransferable;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联的服务项目列表 */
    private List<ServiceItemRelVO> serviceItems;

    /**
     * 服务项目关联 VO（含联查的 service_item 名称/大类等信息）。
     */
    @Data
    public static class ServiceItemRelVO {
        private Long id;
        private String goodsCode;
        private String itemCode;
        private String itemName;
        private Integer itemCategory;
        private Integer itemSubtype;
        private Integer quantity;
        /** 配额周期（1=终身总量,2=年度配额） */
        private Integer quotaType;
        private Integer sortOrder;
        private LocalDateTime createdAt;
    }
}
