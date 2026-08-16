package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.LearningContentCreateDTO;
import com.dayan.agent.dto.LearningContentQueryDTO;
import com.dayan.agent.dto.LearningContentUpdateDTO;
import com.dayan.agent.entity.LearningContent;
import com.dayan.agent.mapper.LearningContentMapper;
import com.dayan.agent.service.LearningContentService;
import com.dayan.agent.vo.LearningContentAdminVO;
import com.dayan.agent.vo.LearningContentVO;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学习中心内容服务实现。
 *
 * <p>内容编码生成：{@code "LC" + String.format("%05d", sequenceProvider.next("code:seq:LC:0"))}，全表唯一
 * （存量 seed 为 LC+日期长码，两段式互不冲突）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningContentServiceImpl implements LearningContentService {

    /** 内容编码前缀 */
    private static final String CODE_PREFIX = "LC";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:LC:0";

    private final LearningContentMapper learningContentMapper;
    private final SequenceProvider sequenceProvider;

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

        LearningContentVO vo = toVO(content);
        vo.setBody(content.getBody());
        return vo;
    }

    @Override
    public PageResult<LearningContentAdminVO> page(LearningContentQueryDTO query) {
        LambdaQueryWrapper<LearningContent> wrapper = new LambdaQueryWrapper<LearningContent>()
                .like(query.getTitle() != null && !query.getTitle().isBlank(),
                        LearningContent::getTitle, query.getTitle())
                .eq(query.getCategory() != null, LearningContent::getCategory, query.getCategory())
                .eq(query.getStatus() != null, LearningContent::getStatus, query.getStatus())
                .orderByDesc(LearningContent::getSortOrder)
                .orderByDesc(LearningContent::getPublishTime);
        Page<LearningContent> page = learningContentMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<LearningContentAdminVO> records = page.getRecords().stream().map(this::toAdminVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public Long create(LearningContentCreateDTO dto) {
        LearningContent content = new LearningContent();
        content.setContentCode(generateContentCode());
        applyFields(content, dto.getTitle(), dto.getSummary(), dto.getCategory(), dto.getAuthor(),
                dto.getDuration(), dto.getBody(), dto.getBadge(), dto.getPublishTime(),
                dto.getSortOrder(), dto.getStatus() != null ? dto.getStatus() : 1);
        content.setViewCount(0);
        learningContentMapper.insert(content);
        return content.getId();
    }

    @Override
    public void update(Long id, LearningContentUpdateDTO dto) {
        LearningContent content = requireContent(id);
        applyFields(content, dto.getTitle(), dto.getSummary(), dto.getCategory(), dto.getAuthor(),
                dto.getDuration(), dto.getBody(), dto.getBadge(), dto.getPublishTime(),
                dto.getSortOrder(), dto.getStatus());
        learningContentMapper.updateById(content);
    }

    @Override
    public void delete(Long id) {
        requireContent(id);
        learningContentMapper.deleteById(id);
    }

    private LearningContent requireContent(Long id) {
        LearningContent content = learningContentMapper.selectById(id);
        if (content == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在: id=" + id);
        }
        return content;
    }

    /** 建改共用字段回填（status 传 null 时保持原值） */
    private void applyFields(LearningContent content, String title, String summary, Integer category,
                             String author, String duration, String body, String badge,
                             java.time.LocalDateTime publishTime, Integer sortOrder, Integer status) {
        content.setTitle(title);
        content.setSummary(summary);
        content.setCategory(category);
        content.setAuthor(author);
        content.setDuration(duration);
        content.setBody(body);
        content.setBadge(badge);
        content.setPublishTime(publishTime);
        content.setSortOrder(sortOrder != null ? sortOrder : 0);
        if (status != null) {
            content.setStatus(status);
        }
    }

    private String generateContentCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
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

    private LearningContentAdminVO toAdminVO(LearningContent c) {
        LearningContentAdminVO vo = new LearningContentAdminVO();
        vo.setId(c.getId());
        vo.setContentCode(c.getContentCode());
        vo.setTitle(c.getTitle());
        vo.setSummary(c.getSummary());
        vo.setCategory(c.getCategory());
        vo.setAuthor(c.getAuthor());
        vo.setDuration(c.getDuration());
        vo.setBody(c.getBody());
        vo.setViewCount(c.getViewCount());
        vo.setBadge(c.getBadge());
        vo.setPublishTime(c.getPublishTime());
        vo.setSortOrder(c.getSortOrder());
        vo.setStatus(c.getStatus());
        vo.setCreatedAt(c.getCreatedAt());
        vo.setUpdatedAt(c.getUpdatedAt());
        return vo;
    }
}
