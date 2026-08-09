package com.dayan.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.goods.entity.GoodsServiceItemRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoodsServiceItemRelMapper extends BaseMapper<GoodsServiceItemRel> {

    /**
     * 物理删除指定商品的所有关联（绕过逻辑删除，用于先删后插场景）。
     * 逻辑删除 + 唯一键(goods_code, item_code, deleted) 会在反复保存时冲突，
     * 因此纯关联表用物理删除。
     */
    @Delete("DELETE FROM goods_service_item_rel WHERE goods_code = #{goodsCode}")
    int physicalDeleteByGoodsCode(@Param("goodsCode") String goodsCode);
}

