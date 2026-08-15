package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsDisplayBlockCreateDTO;
import com.dayan.goods.dto.GoodsDisplayBlockQueryDTO;
import com.dayan.goods.dto.GoodsDisplayBlockUpdateDTO;
import com.dayan.goods.vo.GoodsDisplayBlockVO;

import java.util.List;

/**
 * 商品展示板块（goods_display_block）服务。
 */
public interface GoodsDisplayBlockService {

    PageResult<GoodsDisplayBlockVO> page(GoodsDisplayBlockQueryDTO query);

    /** 商品可见板块列表（status=1，按 sortOrder 升序）——C/Agent 端详情页 tab 数据源 */
    List<GoodsDisplayBlockVO> listByGoods(String goodsCode);

    GoodsDisplayBlockVO getDetail(Long id);

    Long create(GoodsDisplayBlockCreateDTO dto);

    void update(Long id, GoodsDisplayBlockUpdateDTO dto);

    void delete(Long id);
}
