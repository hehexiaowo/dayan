package com.dayan.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.goods.entity.GoodsEquity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoodsEquityMapper extends BaseMapper<GoodsEquity> {

    /**
     * 物理删除指定商品的权益配置（绕过逻辑删除，用于先删后插/重置场景）。
     * 逻辑删除 + 唯一键(goods_code, deleted) 在反复保存/删除时冲突，用物理删除彻底清除。
     */
    @Delete("DELETE FROM goods_equity WHERE goods_code = #{goodsCode}")
    int physicalDeleteByGoodsCode(@Param("goodsCode") String goodsCode);
}

