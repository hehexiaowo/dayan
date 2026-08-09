package com.dayan.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 权益商品配置保存 DTO（UPSERT：goods_equity 1:1 + goods_service_item_rel N:M 先删后插）。
 */
@Data
public class GoodsEquitySaveDTO {

    /** 商品编码（1:1关联goods_info，必填，不可改） */
    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;

    /** 使用人人数（1=个人版,2=双人版,3+家庭版） */
    @NotNull(message = "使用人人数不能为空")
    private Integer personCount;

    /** 激活后有效天数 */
    @NotNull(message = "有效天数不能为空")
    private Integer validDays;

    /** 库存有效期天数（未激活时） */
    private Integer shelfLifeDays;

    /** 是否可转让（0否1是） */
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
        /** 排序号 */
        private Integer sortOrder;
    }
}
