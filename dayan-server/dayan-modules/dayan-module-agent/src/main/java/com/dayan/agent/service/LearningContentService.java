package com.dayan.agent.service;

import com.dayan.agent.vo.LearningContentVO;

import java.util.List;

/**
 * 学习中心内容服务。
 */
public interface LearningContentService {

    /**
     * 按分类查询上架内容列表。
     *
     * @param category 分类（1=视频 2=图文 3=雁鸣），null = 全部
     */
    List<LearningContentVO> listByCategory(Integer category);

    /**
     * 查询单条内容详情（同时累加浏览量）。
     */
    LearningContentVO getDetail(String contentCode);
}
