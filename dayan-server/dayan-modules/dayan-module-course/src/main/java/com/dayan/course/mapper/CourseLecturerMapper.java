package com.dayan.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.course.entity.CourseLecturer;
import org.apache.ibatis.annotations.Mapper;

/**
 * course_lecturer 数据访问层。
 */
@Mapper
public interface CourseLecturerMapper extends BaseMapper<CourseLecturer> {
}
