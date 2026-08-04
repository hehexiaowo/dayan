package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSkuEquityCreateDTO;
import com.dayan.goods.dto.GoodsSkuEquityQueryDTO;
import com.dayan.goods.dto.GoodsSkuEquityUpdateDTO;
import com.dayan.goods.vo.GoodsSkuEquityVO;

import java.util.List;

/**
 * 权益 SKU（goods_sku_equity）服务。
 *
 * <p>按 {@code goodsCode} 维度管理权益 SKU，{@code skuCode} 由系统生成。
 * 关联 {@code templateCode}（权益模板）采用弱校验（仅非空格式校验）。
 */
public interface GoodsSkuEquityService {

    PageResult<GoodsSkuEquityVO> page(GoodsSkuEquityQueryDTO query);

    /** 按商品编码查询全部权益 SKU */
    List<GoodsSkuEquityVO> listByGoods(String goodsCode);

    GoodsSkuEquityVO getDetail(Long id);

    Long create(GoodsSkuEquityCreateDTO dto);

    void update(Long id, GoodsSkuEquityUpdateDTO dto);

    void delete(Long id);
}
