package com.dayan.course.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.course.dto.CourseRecordLearnCreateDTO;
import com.dayan.course.dto.CourseRecordLearnQueryDTO;
import com.dayan.course.dto.CourseRecordLearnUpdateDTO;
import com.dayan.course.vo.CourseRecordLearnVO;

import java.util.List;

/**
 * 学习记录服务（分片表 course_record_learn，雪花ID）。
 *
 * <p>按 courseCode/clientCode 维度 CRUD，记录学习进度。
 */
public interface CourseRecordLearnService {

    PageResult<CourseRecordLearnVO> page(CourseRecordLearnQueryDTO query);

    List<CourseRecordLearnVO> list(CourseRecordLearnQueryDTO query);

    CourseRecordLearnVO getDetail(Long id);

    Long create(CourseRecordLearnCreateDTO dto);

    void update(Long id, CourseRecordLearnUpdateDTO dto);

    void delete(Long id);
}
