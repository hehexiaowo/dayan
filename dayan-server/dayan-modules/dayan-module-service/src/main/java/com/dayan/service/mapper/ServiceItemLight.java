package com.dayan.service.mapper;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 服务项目轻量行（服务会话展示「服务类型=服务项目名称」用）。
 *
 * <p>直读 service_item 表（goods 模块管理，service 模块仅只读查询，
 * 避免 service → goods 模块依赖；与 ButlerInfoView 等跨模块只读模式一致）。
 */
@Data
@TableName("service_item")
public class ServiceItemLight {

    /** 服务项目编码（SI+5位） */
    private String itemCode;

    /** 服务项目名称（如：旅居 / 活力长居 / 照护长居） */
    private String itemName;

    /** 项目大类（1=安排权益，2=费用权益） */
    private Integer itemCategory;

    /** 状态（0=停用,1=启用） */
    private Integer status;
}
