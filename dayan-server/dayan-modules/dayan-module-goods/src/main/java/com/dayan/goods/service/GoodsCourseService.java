package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsCourseCreateDTO;
import com.dayan.goods.dto.GoodsCourseQueryDTO;
import com.dayan.goods.dto.GoodsCourseUpdateDTO;
import com.dayan.goods.vo.GoodsCourseVO;

import java.util.List;

/**
 * 课程 SKU（goods_course）服务。
 *
 * <p>按 {@code goodsCode} 维度管理课程 SKU；{@code courseCode}（课程）弱校验。
 * 学员上限以 {@code stock} 字段承载（库存即学员上限）。
 */
public interface GoodsCourseService {

    PageResult<GoodsCourseVO> page(GoodsCourseQueryDTO query);

    /** 按商品编码查询全部课程 SKU */
    List<GoodsCourseVO> listByGoods(String goodsCode);

    GoodsCourseVO getDetail(Long id);

    /** 按 skuCode 查询单个 SKU（不存在返回 null） */
    GoodsCourseVO getByCode(String skuCode);

    Long create(GoodsCourseCreateDTO dto);

    void update(Long id, GoodsCourseUpdateDTO dto);

    void delete(Long id);
}
