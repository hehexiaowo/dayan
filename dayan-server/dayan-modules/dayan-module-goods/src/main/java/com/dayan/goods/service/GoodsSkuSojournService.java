package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSkuSojournCreateDTO;
import com.dayan.goods.dto.GoodsSkuSojournQueryDTO;
import com.dayan.goods.dto.GoodsSkuSojournUpdateDTO;
import com.dayan.goods.vo.GoodsSkuSojournVO;

import java.util.List;

/**
 * 旅居 SKU（goods_sku_sojourn）服务。
 *
 * <p>按 {@code goodsCode} 维度管理旅居 SKU；{@code parkCode}/{@code roomTypeCode} 弱校验。
 * 时长范围由 {@code minDays}/{@code maxDays} 承载。
 */
public interface GoodsSkuSojournService {

    PageResult<GoodsSkuSojournVO> page(GoodsSkuSojournQueryDTO query);

    /** 按商品编码查询全部旅居 SKU */
    List<GoodsSkuSojournVO> listByGoods(String goodsCode);

    GoodsSkuSojournVO getDetail(Long id);

    /** 按 skuCode 查询单个 SKU（不存在返回 null） */
    GoodsSkuSojournVO getByCode(String skuCode);

    Long create(GoodsSkuSojournCreateDTO dto);

    void update(Long id, GoodsSkuSojournUpdateDTO dto);

    void delete(Long id);
}
