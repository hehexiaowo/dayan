package com.dayan.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.goods.entity.GoodsCourse;
import org.apache.ibatis.annotations.Mapper;

/**
 * goods_course 数据访问层。
 */
@Mapper
public interface GoodsCourseMapper extends BaseMapper<GoodsCourse> {
}
