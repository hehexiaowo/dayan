package com.dayan.goods.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSojournCreateDTO;
import com.dayan.goods.dto.GoodsSojournQueryDTO;
import com.dayan.goods.dto.GoodsSojournUpdateDTO;
import com.dayan.goods.vo.GoodsSojournVO;

import java.util.List;

/**
 * 旅居 SKU（goods_sojourn）服务。
 *
 * <p>按 {@code goodsCode} 维度管理旅居 SKU；{@code parkCode}/{@code roomTypeCode} 弱校验。
 * 时长范围由 {@code minDays}/{@code maxDays} 承载。
 */
public interface GoodsSojournService {

    PageResult<GoodsSojournVO> page(GoodsSojournQueryDTO query);

    /** 按商品编码查询全部旅居 SKU */
    List<GoodsSojournVO> listByGoods(String goodsCode);

    GoodsSojournVO getDetail(Long id);

    /** 按 skuCode 查询单个 SKU（不存在返回 null） */
    GoodsSojournVO getByCode(String skuCode);

    /** 按 skuCode 查有效在售 SKU（status=1 且在有效期内），不存在返回 null */
    GoodsSojournVO getEffectiveByCode(String skuCode);

    Long create(GoodsSojournCreateDTO dto);

    void update(Long id, GoodsSojournUpdateDTO dto);

    void delete(Long id);
}
