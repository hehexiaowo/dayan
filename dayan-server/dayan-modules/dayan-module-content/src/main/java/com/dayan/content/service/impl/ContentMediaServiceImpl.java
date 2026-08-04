package com.dayan.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentMediaCreateDTO;
import com.dayan.content.dto.ContentMediaQueryDTO;
import com.dayan.content.dto.ContentMediaUpdateDTO;
import com.dayan.content.entity.ContentMedia;
import com.dayan.content.mapper.ContentMediaMapper;
import com.dayan.content.service.ContentMediaService;
import com.dayan.content.vo.ContentMediaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 内容多媒体资源服务实现。
 *
 * <p>{@code content_media} 平台共享表，按 {@code contentCode} 归属资源明细。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentMediaServiceImpl implements ContentMediaService {

    private final ContentMediaMapper contentMediaMapper;

    @Override
    public PageResult<ContentMediaVO> page(ContentMediaQueryDTO query) {
        LambdaQueryWrapper<ContentMedia> wrapper = buildWrapper(query.getContentCode(),
                query.getMediaType());
        Page<ContentMedia> page = contentMediaMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ContentMediaVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ContentMediaVO> listByContentCode(String contentCode) {
        return contentMediaMapper.selectList(buildWrapper(contentCode, null)).stream()
                .map(this::toVO).toList();
    }

    @Override
    public ContentMediaVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ContentMediaCreateDTO dto) {
        ContentMedia entity = new ContentMedia();
        entity.setContentCode(dto.getContentCode());
        entity.setMediaType(dto.getMediaType());
        entity.setMediaUrl(dto.getMediaUrl());
        entity.setThumbnailUrl(dto.getThumbnailUrl());
        entity.setMediaName(dto.getMediaName());
        entity.setFileFormat(dto.getFileFormat());
        entity.setFileSize(dto.getFileSize());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setDuration(dto.getDuration());
        entity.setMediaDescription(dto.getMediaDescription());
        entity.setIsInBody(dto.getIsInBody() == null ? 0 : dto.getIsInBody());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());

        contentMediaMapper.insert(entity);
        log.info("创建多媒体资源成功: id={}, contentCode={}", entity.getId(), dto.getContentCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ContentMediaUpdateDTO dto) {
        ContentMedia existing = requireById(id);
        ContentMedia update = new ContentMedia();
        update.setId(existing.getId());

        if (dto.getMediaType() != null) update.setMediaType(dto.getMediaType());
        if (dto.getMediaUrl() != null) update.setMediaUrl(dto.getMediaUrl());
        if (dto.getThumbnailUrl() != null) update.setThumbnailUrl(dto.getThumbnailUrl());
        if (dto.getMediaName() != null) update.setMediaName(dto.getMediaName());
        if (dto.getFileFormat() != null) update.setFileFormat(dto.getFileFormat());
        if (dto.getFileSize() != null) update.setFileSize(dto.getFileSize());
        if (dto.getWidth() != null) update.setWidth(dto.getWidth());
        if (dto.getHeight() != null) update.setHeight(dto.getHeight());
        if (dto.getDuration() != null) update.setDuration(dto.getDuration());
        if (dto.getMediaDescription() != null) update.setMediaDescription(dto.getMediaDescription());
        if (dto.getIsInBody() != null) update.setIsInBody(dto.getIsInBody());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());

        contentMediaMapper.updateById(update);
        log.info("更新多媒体资源成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        contentMediaMapper.deleteById(id);
        log.info("删除多媒体资源成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByContentCode(String contentCode) {
        contentMediaMapper.delete(new LambdaQueryWrapper<ContentMedia>()
                .eq(ContentMedia::getContentCode, contentCode));
        log.info("按 contentCode 批量删除多媒体资源成功: contentCode={}", contentCode);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ContentMedia> buildWrapper(String contentCode, Integer mediaType) {
        return new LambdaQueryWrapper<ContentMedia>()
                .eq(contentCode != null && !contentCode.isEmpty(),
                        ContentMedia::getContentCode, contentCode)
                .eq(mediaType != null, ContentMedia::getMediaType, mediaType)
                .orderByAsc(ContentMedia::getSortOrder)
                .orderByAsc(ContentMedia::getId);
    }

    private ContentMedia requireById(Long id) {
        ContentMedia entity = contentMediaMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "多媒体资源不存在: id=" + id);
        }
        return entity;
    }

    private ContentMediaVO toVO(ContentMedia entity) {
        ContentMediaVO vo = new ContentMediaVO();
        vo.setId(entity.getId());
        vo.setContentCode(entity.getContentCode());
        vo.setMediaType(entity.getMediaType());
        vo.setMediaUrl(entity.getMediaUrl());
        vo.setThumbnailUrl(entity.getThumbnailUrl());
        vo.setMediaName(entity.getMediaName());
        vo.setFileFormat(entity.getFileFormat());
        vo.setFileSize(entity.getFileSize());
        vo.setWidth(entity.getWidth());
        vo.setHeight(entity.getHeight());
        vo.setDuration(entity.getDuration());
        vo.setMediaDescription(entity.getMediaDescription());
        vo.setIsInBody(entity.getIsInBody());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
