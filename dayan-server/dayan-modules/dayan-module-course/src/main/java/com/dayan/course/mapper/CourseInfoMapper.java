package com.dayan.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.course.entity.CourseInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * course_info 数据访问层。
 */
@Mapper
public interface CourseInfoMapper extends BaseMapper<CourseInfo> {
}
