package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSceneCreateDTO;
import com.dayan.goods.dto.GoodsSceneQueryDTO;
import com.dayan.goods.dto.GoodsSceneUpdateDTO;
import com.dayan.goods.vo.GoodsSceneVO;

import java.util.List;

/**
 * 场景 SKU（goods_scene）服务。
 *
 * <p>按 {@code goodsCode} 维度管理场景 SKU；{@code sceneCode}（场景）弱校验。
 */
public interface GoodsSceneService {

    PageResult<GoodsSceneVO> page(GoodsSceneQueryDTO query);

    /** 按商品编码查询全部场景 SKU */
    List<GoodsSceneVO> listByGoods(String goodsCode);

    GoodsSceneVO getDetail(Long id);

    /** 按 skuCode 查询单个 SKU（不存在返回 null） */
    GoodsSceneVO getByCode(String skuCode);

    Long create(GoodsSceneCreateDTO dto);

    void update(Long id, GoodsSceneUpdateDTO dto);

    void delete(Long id);
}
