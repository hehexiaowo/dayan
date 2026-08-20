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

    // ====== Agent 端只读 ======

    /**
     * 上报学习进度（Agent 端）。
     *
     * <p>若当前 agent+course 无学习记录则自动创建（状态=学习中），
     * 已有记录则累加学习时长、推进课时。返回更新后的记录。
     *
     * @param agentCode      代理人编码
     * @param courseCode      课程编码
     * @param currentLesson   当前课时（可选，null 则不更新）
     * @param learnTimeDelta  本次学习时长增量（分钟）
     */
    CourseRecordLearnVO reportProgress(String agentCode, String courseCode,
                                       Integer currentLesson, Integer learnTimeDelta);

    /**
     * 查询当前 agent 在某课程的学习记录（Agent 端）。
     *
     * @return 记录或 null（未开始学习）
     */
    CourseRecordLearnVO getMyRecord(String agentCode, String courseCode);

    /**
     * 查询当前 agent 的全部学习记录（Agent 端），按最近学习时间倒序。
     */
    List<CourseRecordLearnVO> listMyRecords(String agentCode);
}
