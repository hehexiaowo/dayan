package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSkuSceneCreateDTO;
import com.dayan.goods.dto.GoodsSkuSceneQueryDTO;
import com.dayan.goods.dto.GoodsSkuSceneUpdateDTO;
import com.dayan.goods.vo.GoodsSkuSceneVO;

import java.util.List;

/**
 * 场景 SKU（goods_sku_scene）服务。
 *
 * <p>按 {@code goodsCode} 维度管理场景 SKU；{@code sceneCode}（场景）弱校验。
 */
public interface GoodsSkuSceneService {

    PageResult<GoodsSkuSceneVO> page(GoodsSkuSceneQueryDTO query);

    /** 按商品编码查询全部场景 SKU */
    List<GoodsSkuSceneVO> listByGoods(String goodsCode);

    GoodsSkuSceneVO getDetail(Long id);

    Long create(GoodsSkuSceneCreateDTO dto);

    void update(Long id, GoodsSkuSceneUpdateDTO dto);

    void delete(Long id);
}
