package com.dayan.goods.dto;

import com.dayan.goods.model.HolderRule;
import com.dayan.goods.model.NetworkScope;
import com.dayan.goods.model.UsageRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 权益商品配置保存 DTO（UPSERT：goods_equity 1:1 + goods_service_item_rel N:M 先删后插）。
 */
@Data
public class GoodsEquitySaveDTO {

    /** 商品编码（1:1关联goods_info，必填，不可改） */
    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;

    /** 使用人人数（1=个人版,2=双人版,3+家庭版；须等于 holderRule 构成之和） */
    @NotNull(message = "使用人人数不能为空")
    private Integer personCount;

    /** 权益期限类型（1=固定天数，2=终身），默认1 */
    private Integer validityType;

    /** 权益人构成规则（结构化，落库序列化为 holder_rule JSON） */
    private HolderRule holderRule;

    /** 配额归属（0=按人独立配额，1=权益人共享池），默认1 */
    private Integer shareMode;

    /** 激活后有效天数（validityType=2 终身时不生效） */
    @NotNull(message = "有效天数不能为空")
    private Integer validDays;

    /** 库存有效期天数（未激活时） */
    private Integer shelfLifeDays;

    /** 可转让次数（0=不可转让，1/2/3=可转让N次） */
    private Integer maxTransferable;

    /** 权益配置说明 */
    private String description;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=停用,1=启用） */
    private Integer status;

    /** 关联的服务项目列表（先删后插，为空则清空关联） */
    private List<ServiceItemRelDTO> serviceItems;

    /**
     * 服务项目关联子项（保存时使用）。
     */
    @Data
    public static class ServiceItemRelDTO {
        /** 服务项目编码 */
        @NotBlank(message = "服务项目编码不能为空")
        private String itemCode;
        /** 数量 */
        @NotNull(message = "数量不能为空")
        private Integer quantity;
        /** 配额周期（1=终身总量,2=年度配额（按激活周年重置）），默认2=年度 */
        private Integer quotaType;
        /** 服务网络范围（null=业态全部机构；mode=custom 时按 parkCodes 自选） */
        private NetworkScope networkScope;
        /** 保证入住权（0/1） */
        private Integer admissionGuaranteed;
        /** 优先入住权（0/1） */
        private Integer admissionPriority;
        /** 优惠入住权/旅居优惠权（0/1） */
        private Integer admissionDiscount;
        /** 优惠折扣率（90.00=门市价9折；null=按协议未定） */
        private BigDecimal discountRate;
        /** 单次使用规则（随心住类：晚数/间数/人数/预订/预定金/取消政策/黑名单） */
        private UsageRule usageRule;
        /** 排序号 */
        private Integer sortOrder;
    }
}
