package com.dayan.agent.service;

import com.dayan.agent.dto.LearningContentCreateDTO;
import com.dayan.agent.dto.LearningContentQueryDTO;
import com.dayan.agent.dto.LearningContentUpdateDTO;
import com.dayan.agent.vo.LearningContentAdminVO;
import com.dayan.agent.vo.LearningContentVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 学习中心内容服务。
 */
public interface LearningContentService {

    /**
     * 按板块查询上架内容列表（Agent 端）。
     *
     * @param category 板块分类（1=渠道课程 2=外部课程 3=雁鸣中国），null = 全部
     */
    List<LearningContentVO> listByCategory(Integer category);

    /**
     * 查询单条内容详情（同时累加浏览量，Agent 端）。
     */
    LearningContentVO getDetail(String contentCode);

    /**
     * 内容分页列表（Admin 端，含下架）。
     */
    PageResult<LearningContentAdminVO> page(LearningContentQueryDTO query);

    /**
     * 新增内容（Admin 端），返回自增 id。contentCode 系统生成。
     */
    Long create(LearningContentCreateDTO dto);

    /**
     * 修改内容（Admin 端，id 走 path）。
     */
    void update(Long id, LearningContentUpdateDTO dto);

    /**
     * 删除内容（Admin 端，逻辑删除）。
     */
    void delete(Long id);
}
