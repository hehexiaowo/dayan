package com.dayan.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentRecordShareCreateDTO;
import com.dayan.content.dto.ContentRecordShareQueryDTO;
import com.dayan.content.dto.ContentRecordShareUpdateDTO;
import com.dayan.content.entity.ContentRecordShare;
import com.dayan.content.mapper.ContentRecordShareMapper;
import com.dayan.content.service.ContentRecordShareService;
import com.dayan.content.vo.ContentRecordShareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容分享记录服务实现。
 *
 * <p>{@code content_record_share} 分片表（ASSIGN_ID 主键）。记录分享事件，支持按 contentCode
 * 查询与转化数据回填。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentRecordShareServiceImpl implements ContentRecordShareService {

    private final ContentRecordShareMapper contentRecordShareMapper;

    @Override
    public PageResult<ContentRecordShareVO> page(ContentRecordShareQueryDTO query) {
        LambdaQueryWrapper<ContentRecordShare> wrapper = buildWrapper(query);
        Page<ContentRecordShare> page = contentRecordShareMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ContentRecordShareVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public ContentRecordShareVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ContentRecordShareCreateDTO dto) {
        ContentRecordShare entity = new ContentRecordShare();
        entity.setContentCode(dto.getContentCode());
        entity.setSharerType(dto.getSharerType());
        entity.setSharerCode(dto.getSharerCode());
        entity.setShareChannel(dto.getShareChannel());
        entity.setShareUrl(dto.getShareUrl());
        entity.setShareTitle(dto.getShareTitle());
        entity.setShareDescription(dto.getShareDescription());
        entity.setShareImage(dto.getShareImage());
        entity.setClickCount(0);
        entity.setConvertCount(0);
        entity.setShareTime(dto.getShareTime() == null ? LocalDateTime.now() : dto.getShareTime());

        contentRecordShareMapper.insert(entity);
        log.info("记录分享事件成功: id={}, contentCode={}, sharerCode={}",
                entity.getId(), dto.getContentCode(), dto.getSharerCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ContentRecordShareUpdateDTO dto) {
        ContentRecordShare existing = requireById(id);
        ContentRecordShare update = new ContentRecordShare();
        update.setId(existing.getId());

        if (dto.getClickCount() != null) update.setClickCount(dto.getClickCount());
        if (dto.getConvertCount() != null) update.setConvertCount(dto.getConvertCount());

        contentRecordShareMapper.updateById(update);
        log.info("回填分享记录数据成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        contentRecordShareMapper.deleteById(id);
        log.info("删除分享记录成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ContentRecordShare> buildWrapper(ContentRecordShareQueryDTO query) {
        return new LambdaQueryWrapper<ContentRecordShare>()
                .eq(query.getContentCode() != null && !query.getContentCode().isEmpty(),
                        ContentRecordShare::getContentCode, query.getContentCode())
                .eq(query.getSharerCode() != null && !query.getSharerCode().isEmpty(),
                        ContentRecordShare::getSharerCode, query.getSharerCode())
                .eq(query.getShareChannel() != null,
                        ContentRecordShare::getShareChannel, query.getShareChannel())
                .orderByDesc(ContentRecordShare::getShareTime);
    }

    private ContentRecordShare requireById(Long id) {
        ContentRecordShare entity = contentRecordShareMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分享记录不存在: id=" + id);
        }
        return entity;
    }

    private ContentRecordShareVO toVO(ContentRecordShare entity) {
        ContentRecordShareVO vo = new ContentRecordShareVO();
        vo.setId(entity.getId());
        vo.setContentCode(entity.getContentCode());
        vo.setSharerType(entity.getSharerType());
        vo.setSharerCode(entity.getSharerCode());
        vo.setShareChannel(entity.getShareChannel());
        vo.setShareUrl(entity.getShareUrl());
        vo.setShareTitle(entity.getShareTitle());
        vo.setShareDescription(entity.getShareDescription());
        vo.setShareImage(entity.getShareImage());
        vo.setClickCount(entity.getClickCount());
        vo.setConvertCount(entity.getConvertCount());
        vo.setShareTime(entity.getShareTime());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
