package com.dayan.course.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.course.dto.CourseLecturerCreateDTO;
import com.dayan.course.dto.CourseLecturerQueryDTO;
import com.dayan.course.dto.CourseLecturerUpdateDTO;
import com.dayan.course.vo.CourseLecturerVO;

import java.util.List;

/**
 * 课程讲师服务。
 *
 * <p>{@code lecturerCode} 由系统生成（LT + 5 位序列），全表唯一，作为 course_info 的关联键。
 * CRUD 以 id 为主键定位。
 */
public interface CourseLecturerService {

    PageResult<CourseLecturerVO> page(CourseLecturerQueryDTO query);

    List<CourseLecturerVO> list(CourseLecturerQueryDTO query);

    CourseLecturerVO getDetail(Long id);

    String create(CourseLecturerCreateDTO dto);

    void update(Long id, CourseLecturerUpdateDTO dto);

    void delete(Long id);
}
