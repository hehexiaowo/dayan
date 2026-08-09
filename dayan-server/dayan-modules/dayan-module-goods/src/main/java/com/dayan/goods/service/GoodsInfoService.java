package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsInfoCreateDTO;
import com.dayan.goods.dto.GoodsInfoQueryDTO;
import com.dayan.goods.dto.GoodsInfoShelfDTO;
import com.dayan.goods.dto.GoodsInfoUpdateDTO;
import com.dayan.goods.entity.GoodsInfo;
import com.dayan.goods.vo.GoodsInfoVO;

import java.util.List;

/**
 * 商品 SPU（goods_info）服务。
 *
 * <p>{@code goodsCode} 由 {@code "GD" + 5 位序列} 生成，全局唯一。
 * 商品类型 {@code goodsType}（1权益/2场景/3课程/4旅居）决定关联哪类 SKU 子表，
 * 类型创建后不可变更。
 */
public interface GoodsInfoService {

    /**
     * 分页查询商品列表。
     */
    PageResult<GoodsInfoVO> page(GoodsInfoQueryDTO query);

    /**
     * 列表查询（轻量，不分页）。
     */
    List<GoodsInfoVO> list(GoodsInfoQueryDTO query);

    /**
     * 商品详情（按 goodsCode）。
     */
    GoodsInfoVO getDetail(String goodsCode);

    /**
     * 新增商品，返回生成的 goodsCode。
     */
    String create(GoodsInfoCreateDTO dto);

    /**
     * 修改商品（按 goodsCode）。不允许修改 goodsType。
     */
    void update(String goodsCode, GoodsInfoUpdateDTO dto);

    /**
     * 上下架商品。
     */
    void shelf(GoodsInfoShelfDTO dto);

    /**
     * 删除商品（按 goodsCode）。校验是否存在关联 SKU。
     */
    void delete(String goodsCode);

    /**
     * 内部调用：获取商品实体（供权益激活链路取 goodsName/costPrice 等）。
     *
     * @param goodsCode 商品编码
     * @return 商品实体（不存在则抛异常）
     */
    GoodsInfo requireGoods(String goodsCode);
}
