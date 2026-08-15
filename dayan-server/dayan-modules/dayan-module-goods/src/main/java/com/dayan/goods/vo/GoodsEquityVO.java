package com.dayan.goods.vo;

import com.dayan.goods.model.HolderRule;
import com.dayan.goods.model.NetworkScope;
import com.dayan.goods.model.UsageRule;
import lombok.Data;

import java.math.BigDecimal;
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
    /** 权益期限类型（1=固定天数，2=终身） */
    private Integer validityType;
    /** 权益人构成规则（结构化） */
    private HolderRule holderRule;
    /** 配额归属（0=按人独立配额，1=权益人共享池） */
    private Integer shareMode;
    private Integer validDays;
    private Integer shelfLifeDays;
    /** 可转让次数（0=不可转让，N=可转让N次） */
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
        /** 配额周期（1=终身总量,2=年度配额（按激活周年重置）） */
        private Integer quotaType;
        /** 服务网络范围（null=业态全部机构） */
        private NetworkScope networkScope;
        /** 保证入住权（0/1） */
        private Integer admissionGuaranteed;
        /** 优先入住权（0/1） */
        private Integer admissionPriority;
        /** 优惠入住权/旅居优惠权（0/1） */
        private Integer admissionDiscount;
        /** 优惠折扣率（90.00=门市价9折；null=按协议未定） */
        private BigDecimal discountRate;
        /** 单次使用规则（随心住类） */
        private UsageRule usageRule;
        private Integer sortOrder;
        private LocalDateTime createdAt;
    }
}
