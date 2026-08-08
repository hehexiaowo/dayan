package com.dayan.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentRecordReadCreateDTO;
import com.dayan.content.dto.ContentRecordReadQueryDTO;
import com.dayan.content.entity.ContentRecordRead;
import com.dayan.content.mapper.ContentRecordReadMapper;
import com.dayan.content.service.ContentRecordReadService;
import com.dayan.content.vo.ContentReadStatsVO;
import com.dayan.content.vo.ContentRecordReadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容阅读记录服务实现。
 *
 * <p>{@code content_record_read} 分片表（ASSIGN_ID 主键）。
 * 统计口径：PV = 按 contentCode 的记录总条数；UV = 按 readerCode 去重（{@link ContentRecordReadMapper#countUvByContentCode}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentRecordReadServiceImpl implements ContentRecordReadService {

    private final ContentRecordReadMapper contentRecordReadMapper;

    @Override
    public PageResult<ContentRecordReadVO> page(ContentRecordReadQueryDTO query) {
        LambdaQueryWrapper<ContentRecordRead> wrapper = buildWrapper(
                query.getContentCode(), query.getReaderCode(), query.getReadSource(),
                query.getContentCodes());
        Page<ContentRecordRead> page = contentRecordReadMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ContentRecordReadVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ContentRecordReadCreateDTO dto) {
        ContentRecordRead entity = new ContentRecordRead();
        entity.setContentCode(dto.getContentCode());
        entity.setReaderType(dto.getReaderType());
        entity.setReaderCode(dto.getReaderCode());
        entity.setReadDuration(dto.getReadDuration());
        entity.setReadProgress(dto.getReadProgress());
        entity.setReadSource(dto.getReadSource());
        entity.setIpAddress(dto.getIpAddress());
        entity.setDeviceType(dto.getDeviceType());
        entity.setReadTime(dto.getReadTime() == null ? LocalDateTime.now() : dto.getReadTime());

        contentRecordReadMapper.insert(entity);
        log.info("记录阅读事件成功: id={}, contentCode={}, readerCode={}",
                entity.getId(), dto.getContentCode(), dto.getReaderCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ContentRecordRead existing = contentRecordReadMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "阅读记录不存在: id=" + id);
        }
        contentRecordReadMapper.deleteById(id);
        log.info("删除阅读记录成功: id={}", id);
    }

    @Override
    public ContentReadStatsVO stats(String contentCode) {
        // PV：记录总条数
        Long pv = contentRecordReadMapper.selectCount(new LambdaQueryWrapper<ContentRecordRead>()
                .eq(ContentRecordRead::getContentCode, contentCode));
        // UV：按 readerCode 去重
        long uv = contentRecordReadMapper.countUvByContentCode(contentCode);

        ContentReadStatsVO vo = new ContentReadStatsVO();
        vo.setContentCode(contentCode);
        vo.setPv(pv == null ? 0L : pv);
        vo.setUv(uv);
        return vo;
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ContentRecordRead> buildWrapper(String contentCode,
                                                               String readerCode,
                                                               Integer readSource,
                                                               java.util.List<String> contentCodes) {
        LambdaQueryWrapper<ContentRecordRead> wrapper = new LambdaQueryWrapper<ContentRecordRead>()
                .eq(contentCode != null && !contentCode.isEmpty(),
                        ContentRecordRead::getContentCode, contentCode)
                .eq(readerCode != null && !readerCode.isEmpty(),
                        ContentRecordRead::getReaderCode, readerCode)
                .eq(readSource != null, ContentRecordRead::getReadSource, readSource)
                .orderByDesc(ContentRecordRead::getReadTime);
        if (contentCodes != null && !contentCodes.isEmpty()) {
            wrapper.in(ContentRecordRead::getContentCode, contentCodes);
        }
        return wrapper;
    }

    private ContentRecordReadVO toVO(ContentRecordRead entity) {
        ContentRecordReadVO vo = new ContentRecordReadVO();
        vo.setId(entity.getId());
        vo.setContentCode(entity.getContentCode());
        vo.setReaderType(entity.getReaderType());
        vo.setReaderCode(entity.getReaderCode());
        vo.setReadDuration(entity.getReadDuration());
        vo.setReadProgress(entity.getReadProgress());
        vo.setReadSource(entity.getReadSource());
        vo.setIpAddress(entity.getIpAddress());
        vo.setDeviceType(entity.getDeviceType());
        vo.setReadTime(entity.getReadTime());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
