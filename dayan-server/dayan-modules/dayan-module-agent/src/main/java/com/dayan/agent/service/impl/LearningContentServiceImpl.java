package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.agent.entity.LearningContent;
import com.dayan.agent.mapper.LearningContentMapper;
import com.dayan.agent.service.LearningContentService;
import com.dayan.agent.vo.LearningContentVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学习中心内容服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningContentServiceImpl implements LearningContentService {

    private final LearningContentMapper learningContentMapper;

    @Override
    public List<LearningContentVO> listByCategory(Integer category) {
        LambdaQueryWrapper<LearningContent> wrapper = new LambdaQueryWrapper<LearningContent>()
                .eq(LearningContent::getStatus, 1)
                .eq(category != null, LearningContent::getCategory, category)
                .orderByDesc(LearningContent::getSortOrder)
                .orderByDesc(LearningContent::getPublishTime);
        return learningContentMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public LearningContentVO getDetail(String contentCode) {
        LearningContent content = learningContentMapper.selectOne(
                new LambdaQueryWrapper<LearningContent>()
                        .eq(LearningContent::getContentCode, contentCode)
                        .eq(LearningContent::getStatus, 1)
                        .last("LIMIT 1"));
        if (content == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在: " + contentCode);
        }

        // 浏览量 +1（非事务关键路径，失败忽略）
        try {
            content.setViewCount((content.getViewCount() != null ? content.getViewCount() : 0) + 1);
            learningContentMapper.updateById(content);
        } catch (Exception e) {
            log.warn("[Learning] 浏览量累加失败 contentCode={}", contentCode, e);
        }

        return toVO(content);
    }

    private LearningContentVO toVO(LearningContent c) {
        LearningContentVO vo = new LearningContentVO();
        vo.setId(c.getId());
        vo.setContentCode(c.getContentCode());
        vo.setTitle(c.getTitle());
        vo.setSummary(c.getSummary());
        vo.setCategory(c.getCategory());
        vo.setAuthor(c.getAuthor());
        vo.setDuration(c.getDuration());
        vo.setViewCount(c.getViewCount());
        vo.setBadge(c.getBadge());
        vo.setPublishTime(c.getPublishTime());
        vo.setSortOrder(c.getSortOrder());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }
}
