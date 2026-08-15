package com.dayan.goods.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Agent 代理人端商品 VO（基础信息 + 权益配置 + 页面展示内容聚合）。
 *
 * <p>继承 {@link GoodsInfoVO} 保持 JSON 扁平兼容（前端原有 goodsName/salePrice 等
 * 引用不变），追加 {@code equity} 携带权益内容（权益人构成/期限/共享/转让 +
 * 服务项目组合的次数/入住权/折扣/随心住规则/网络范围），供代理人展业讲解；
 * 追加 {@code displayBlocks} 携带 C/Agent 端详情页结构化展示板块（产品介绍/
 * 权益详解/服务流程/常见问题/购买须知，status=1 按 sortOrder 升序）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentGoodsVO extends GoodsInfoVO {

    /** 权益配置（goods_equity + 服务项目组合；null=该商品未配置权益内容） */
    private GoodsEquityVO equity;

    /** 页面展示板块（goods_display_block，仅显示态，按 sortOrder 升序） */
    private List<GoodsDisplayBlockVO> displayBlocks;
}
