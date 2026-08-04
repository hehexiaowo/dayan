package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSkuCourseCreateDTO;
import com.dayan.goods.dto.GoodsSkuCourseQueryDTO;
import com.dayan.goods.dto.GoodsSkuCourseUpdateDTO;
import com.dayan.goods.vo.GoodsSkuCourseVO;

import java.util.List;

/**
 * 课程 SKU（goods_sku_course）服务。
 *
 * <p>按 {@code goodsCode} 维度管理课程 SKU；{@code courseCode}（课程）弱校验。
 * 学员上限以 {@code stock} 字段承载（库存即学员上限）。
 */
public interface GoodsSkuCourseService {

    PageResult<GoodsSkuCourseVO> page(GoodsSkuCourseQueryDTO query);

    /** 按商品编码查询全部课程 SKU */
    List<GoodsSkuCourseVO> listByGoods(String goodsCode);

    GoodsSkuCourseVO getDetail(Long id);

    Long create(GoodsSkuCourseCreateDTO dto);

    void update(Long id, GoodsSkuCourseUpdateDTO dto);

    void delete(Long id);
}
