package com.dayan.goods.service;

import com.dayan.goods.dto.GoodsEquitySaveDTO;
import com.dayan.goods.entity.GoodsEquity;
import com.dayan.goods.vo.GoodsEquityVO;

import java.util.List;

/**
 * 权益商品配置服务（管理 goods_equity 1:1 + goods_service_item_rel N:M）。
 */
public interface GoodsEquityService {

    /**
     * 获取商品权益配置（含关联的服务项目列表）。
     *
     * @param goodsCode 商品编码
     * @return 权益配置 VO（不存在则返回 null）
     */
    GoodsEquityVO getByGoodsCode(String goodsCode);

    /**
     * 保存权益配置（UPSERT：goods_equity 不存在则新建，存在则更新；rel 先删后插）。
     *
     * @param dto 保存 DTO
     */
    void save(GoodsEquitySaveDTO dto);

    /**
     * 删除权益配置（同时删除关联的 rel）。
     *
     * @param goodsCode 商品编码
     */
    void delete(String goodsCode);

    /**
     * 内部调用：获取权益配置实体（供激活链路取 personCount/validDays/shelfLifeDays）。
     *
     * @param goodsCode 商品编码
     * @return 权益配置实体（不存在则抛异常）
     */
    GoodsEquity requireByGoodsCode(String goodsCode);

    /**
     * 内部调用：获取商品关联的服务项目编码列表（供激活链路按 service_item 创建服务会话）。
     *
     * @param goodsCode 商品编码
     * @return 关联的服务项目编码 + 数量列表
     */
    List<GoodsEquityVO.ServiceItemRelVO> listRelsByGoodsCode(String goodsCode);
}
