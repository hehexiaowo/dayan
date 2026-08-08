package com.dayan.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.content.dto.ContentInfoAuditDTO;
import com.dayan.content.dto.ContentInfoCreateDTO;
import com.dayan.content.dto.ContentInfoQueryDTO;
import com.dayan.content.dto.ContentInfoUpdateDTO;
import com.dayan.content.entity.ContentInfo;
import com.dayan.content.enums.ContentEvent;
import com.dayan.content.mapper.ContentInfoMapper;
import com.dayan.content.service.ContentInfoService;
import com.dayan.content.vo.ContentInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容信息服务实现。
 *
 * <p>审核流（contentStatus 状态流转，经 CONTENT_SM 状态机校验）：
 * <ul>
 *   <li>{@code create}：初始 0（草稿）</li>
 *   <li>{@code submit}：0 草稿 → 1 待审（{@link ContentEvent#SUBMIT}）</li>
 *   <li>{@code audit}：1 待审 → 2 通过 / 3 拒绝（{@link ContentEvent#AUDIT_PASS}/{@link ContentEvent#AUDIT_REJECT}）</li>
 *   <li>{@code publish}：2 通过 → 正式上线（保持 contentStatus=2，置 publishTime；自环不经状态机）</li>
 *   <li>{@code offline}：2 通过 → 4 下线（{@link ContentEvent#OFFLINE}）</li>
 * </ul>
 *
 * <p>约束：{@code title} 全表唯一；{@code contentCode}（CT 前缀）由 {@link SequenceProvider} 生成。
 *
 * <p>{@code content_info} 为平台共享表（DayanTenantHandler 忽略），不受渠道字段隔离约束。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentInfoServiceImpl implements ContentInfoService {

    /** 内容编码前缀 */
    private static final String CODE_PREFIX = "CT";
    /** 内容编码序列键（全局共享计数，channelCode 维度传 0） */
    private static final String SEQ_KEY = "code:seq:CT:0";

    private final ContentInfoMapper contentInfoMapper;
    private final SequenceProvider sequenceProvider;
    private final StateMachineEngine stateMachineEngine;

    @Override
    public PageResult<ContentInfoVO> page(ContentInfoQueryDTO query) {
        LambdaQueryWrapper<ContentInfo> wrapper = new LambdaQueryWrapper<ContentInfo>()
                .eq(query.getContentCode() != null && !query.getContentCode().isEmpty(),
                        ContentInfo::getContentCode, query.getContentCode())
                .like(query.getTitle() != null && !query.getTitle().isEmpty(),
                        ContentInfo::getTitle, query.getTitle())
                .eq(query.getContentType() != null,
                        ContentInfo::getContentType, query.getContentType())
                .eq(query.getCategoryCode() != null && !query.getCategoryCode().isEmpty(),
                        ContentInfo::getCategoryCode, query.getCategoryCode())
                .like(query.getAuthorName() != null && !query.getAuthorName().isEmpty(),
                        ContentInfo::getAuthorName, query.getAuthorName())
                .eq(query.getContentStatus() != null,
                        ContentInfo::getContentStatus, query.getContentStatus())
                .eq(query.getAuditStatus() != null,
                        ContentInfo::getAuditStatus, query.getAuditStatus())
                .eq(query.getIsTop() != null, ContentInfo::getIsTop, query.getIsTop())
                .eq(query.getIsRecommend() != null, ContentInfo::getIsRecommend, query.getIsRecommend())
                .orderByDesc(ContentInfo::getCreatedAt);
        if (query.getContentCodes() != null && !query.getContentCodes().isEmpty()) {
            wrapper.in(ContentInfo::getContentCode, query.getContentCodes());
        }
        Page<ContentInfo> page = contentInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ContentInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public ContentInfoVO getDetail(String contentCode) {
        return toVO(requireContent(contentCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ContentInfoCreateDTO dto) {
        // title 全表唯一校验
        checkTitleUnique(dto.getTitle(), null);

        String contentCode = generateContentCode();

        ContentInfo entity = new ContentInfo();
        entity.setContentCode(contentCode);
        entity.setTitle(dto.getTitle());
        entity.setSubtitle(dto.getSubtitle());
        entity.setContentType(dto.getContentType());
        entity.setCategoryCode(dto.getCategoryCode());
        entity.setAuthorName(dto.getAuthorName());
        entity.setAuthorAvatar(dto.getAuthorAvatar());
        entity.setCoverImage(dto.getCoverImage());
        entity.setSummary(dto.getSummary());
        entity.setContentBody(dto.getContentBody());
        entity.setSourceType(dto.getSourceType());
        entity.setSourceUrl(dto.getSourceUrl());
        entity.setTags(dto.getTags());
        entity.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        entity.setIsRecommend(dto.getIsRecommend() == null ? 0 : dto.getIsRecommend());
        entity.setIsComment(dto.getIsComment() == null ? 1 : dto.getIsComment());
        entity.setViewCount(0);
        entity.setLikeCount(0);
        entity.setCommentCount(0);
        entity.setShareCount(0);
        entity.setCollectCount(0);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setContentStatus(ContentEvent.STATUS_DRAFT);
        entity.setAuditStatus(0);
        entity.setRemark(dto.getRemark());

        contentInfoMapper.insert(entity);
        log.info("创建内容成功: contentCode={}, title={}", contentCode, dto.getTitle());
        return contentCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String contentCode, ContentInfoUpdateDTO dto) {
        ContentInfo existing = requireContent(contentCode);
        ContentInfo update = new ContentInfo();
        update.setId(existing.getId());

        if (dto.getTitle() != null) {
            checkTitleUnique(dto.getTitle(), contentCode);
            update.setTitle(dto.getTitle());
        }
        if (dto.getSubtitle() != null) update.setSubtitle(dto.getSubtitle());
        if (dto.getContentType() != null) update.setContentType(dto.getContentType());
        if (dto.getCategoryCode() != null) update.setCategoryCode(dto.getCategoryCode());
        if (dto.getAuthorName() != null) update.setAuthorName(dto.getAuthorName());
        if (dto.getAuthorAvatar() != null) update.setAuthorAvatar(dto.getAuthorAvatar());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getSummary() != null) update.setSummary(dto.getSummary());
        if (dto.getContentBody() != null) update.setContentBody(dto.getContentBody());
        if (dto.getSourceType() != null) update.setSourceType(dto.getSourceType());
        if (dto.getSourceUrl() != null) update.setSourceUrl(dto.getSourceUrl());
        if (dto.getTags() != null) update.setTags(dto.getTags());
        if (dto.getIsTop() != null) update.setIsTop(dto.getIsTop());
        if (dto.getIsRecommend() != null) update.setIsRecommend(dto.getIsRecommend());
        if (dto.getIsComment() != null) update.setIsComment(dto.getIsComment());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        contentInfoMapper.updateById(update);
        log.info("更新内容成功: contentCode={}", contentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String contentCode) {
        requireContent(contentCode);
        contentInfoMapper.delete(new LambdaQueryWrapper<ContentInfo>()
                .eq(ContentInfo::getContentCode, contentCode));
        log.info("删除内容成功: contentCode={}", contentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(String contentCode) {
        ContentInfo existing = requireContent(contentCode);
        int from = existing.getContentStatus() == null ? ContentEvent.STATUS_DRAFT : existing.getContentStatus();
        int to = stateMachineEngine.transition(ContentEvent.DOMAIN, from, ContentEvent.SUBMIT);
        ContentInfo update = new ContentInfo();
        update.setId(existing.getId());
        update.setContentStatus(to);
        contentInfoMapper.updateById(update);
        log.info("内容提交审核成功: contentCode={}", contentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(ContentInfoAuditDTO dto) {
        ContentInfo existing = requireContent(dto.getContentCode());
        Integer auditStatus = dto.getAuditStatus();
        // 业务校验：auditStatus 取值仅允许 2=通过 / 3=拒绝
        if (auditStatus == null || (auditStatus != ContentEvent.STATUS_PASS && auditStatus != ContentEvent.STATUS_REJECT)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "审核状态非法（仅支持 2=通过 / 3=拒绝）");
        }
        // 当前状态合法性（需为待审）交给状态机：非法转移引擎抛 BusinessException
        int from = existing.getContentStatus() == null ? ContentEvent.STATUS_DRAFT : existing.getContentStatus();
        String event = (auditStatus == ContentEvent.STATUS_PASS) ? ContentEvent.AUDIT_PASS : ContentEvent.AUDIT_REJECT;
        int to = stateMachineEngine.transition(ContentEvent.DOMAIN, from, event);

        ContentInfo update = new ContentInfo();
        update.setId(existing.getId());
        update.setContentStatus(to);
        update.setAuditStatus(auditStatus);
        contentInfoMapper.updateById(update);
        log.info("审核内容完成: contentCode={}, auditStatus={}", dto.getContentCode(), auditStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(String contentCode) {
        ContentInfo existing = requireContent(contentCode);
        if (existing.getContentStatus() == null || existing.getContentStatus() != ContentEvent.STATUS_PASS) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "内容当前状态不可发布（需为审核通过状态）: contentCode=" + contentCode);
        }
        ContentInfo update = new ContentInfo();
        update.setId(existing.getId());
        // 保持 contentStatus=2（通过即上线），仅置发布时间（幂等：未发布过才置）
        if (existing.getPublishTime() == null) {
            update.setPublishTime(LocalDateTime.now());
        }
        contentInfoMapper.updateById(update);
        log.info("内容发布成功: contentCode={}", contentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(String contentCode) {
        ContentInfo existing = requireContent(contentCode);
        int from = existing.getContentStatus() == null ? ContentEvent.STATUS_DRAFT : existing.getContentStatus();
        int to = stateMachineEngine.transition(ContentEvent.DOMAIN, from, ContentEvent.OFFLINE);
        ContentInfo update = new ContentInfo();
        update.setId(existing.getId());
        update.setContentStatus(to);
        contentInfoMapper.updateById(update);
        log.info("内容下线成功: contentCode={}", contentCode);
    }

    // ====== 内部方法 ======

    private ContentInfo requireContent(String contentCode) {
        ContentInfo entity = contentInfoMapper.selectOne(new LambdaQueryWrapper<ContentInfo>()
                .eq(ContentInfo::getContentCode, contentCode)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在: " + contentCode);
        }
        return entity;
    }

    /** title 全表唯一校验；{@code excludeCode} 非空时排除自身 */
    private void checkTitleUnique(String title, String excludeCode) {
        if (title == null || title.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<ContentInfo> wrapper = new LambdaQueryWrapper<ContentInfo>()
                .eq(ContentInfo::getTitle, title);
        if (excludeCode != null && !excludeCode.isEmpty()) {
            wrapper.ne(ContentInfo::getContentCode, excludeCode);
        }
        Long count = contentInfoMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "标题已存在");
        }
    }

    /** 生成内容编码：CT + 5 位（左侧补零） */
    private String generateContentCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private ContentInfoVO toVO(ContentInfo entity) {
        ContentInfoVO vo = new ContentInfoVO();
        vo.setId(entity.getId());
        vo.setContentCode(entity.getContentCode());
        vo.setTitle(entity.getTitle());
        vo.setSubtitle(entity.getSubtitle());
        vo.setContentType(entity.getContentType());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setAuthorName(entity.getAuthorName());
        vo.setAuthorAvatar(entity.getAuthorAvatar());
        vo.setCoverImage(entity.getCoverImage());
        vo.setSummary(entity.getSummary());
        vo.setContentBody(entity.getContentBody());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceUrl(entity.getSourceUrl());
        vo.setTags(entity.getTags());
        vo.setIsTop(entity.getIsTop());
        vo.setIsRecommend(entity.getIsRecommend());
        vo.setIsComment(entity.getIsComment());
        vo.setViewCount(entity.getViewCount());
        vo.setLikeCount(entity.getLikeCount());
        vo.setCommentCount(entity.getCommentCount());
        vo.setShareCount(entity.getShareCount());
        vo.setCollectCount(entity.getCollectCount());
        vo.setPublishTime(entity.getPublishTime());
        vo.setSortOrder(entity.getSortOrder());
        vo.setContentStatus(entity.getContentStatus());
        vo.setAuditStatus(entity.getAuditStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
