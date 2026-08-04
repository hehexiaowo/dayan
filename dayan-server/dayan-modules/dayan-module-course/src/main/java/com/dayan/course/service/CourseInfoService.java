package com.dayan.course.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.course.dto.CourseInfoCreateDTO;
import com.dayan.course.dto.CourseInfoQueryDTO;
import com.dayan.course.dto.CourseInfoUpdateDTO;
import com.dayan.course.vo.CourseInfoVO;

import java.util.List;

/**
 * 课程信息服务。
 *
 * <p>{@code courseCode} 由系统生成（CR + 5 位序列），全表唯一。
 * 容量约束：{@code currentStudents ≤ maxStudents}，create/update 均校验。
 */
public interface CourseInfoService {

    PageResult<CourseInfoVO> page(CourseInfoQueryDTO query);

    List<CourseInfoVO> list(CourseInfoQueryDTO query);

    CourseInfoVO getDetail(String courseCode);

    String create(CourseInfoCreateDTO dto);

    void update(String courseCode, CourseInfoUpdateDTO dto);

    void delete(String courseCode);

    /** 上架（courseStatus: 0→1） */
    void publish(String courseCode);

    /** 下架（courseStatus: 1→0） */
    void offline(String courseCode);
}
