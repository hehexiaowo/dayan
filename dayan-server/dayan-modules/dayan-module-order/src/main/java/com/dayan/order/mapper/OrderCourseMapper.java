package com.dayan.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.order.entity.OrderCourse;
import org.apache.ibatis.annotations.Mapper;

/**
 * order_course 数据访问层。
 */
@Mapper
public interface OrderCourseMapper extends BaseMapper<OrderCourse> {
}
