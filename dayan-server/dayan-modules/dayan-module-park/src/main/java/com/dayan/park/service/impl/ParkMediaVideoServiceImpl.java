package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkMediaVideoCreateDTO;
import com.dayan.park.dto.ParkMediaVideoQueryDTO;
import com.dayan.park.dto.ParkMediaVideoUpdateDTO;
import com.dayan.park.entity.ParkMediaVideo;
import com.dayan.park.mapper.ParkMediaVideoMapper;
import com.dayan.park.service.ParkMediaVideoService;
import com.dayan.park.vo.ParkMediaVideoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构视频（park_media_video）服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkMediaVideoServiceImpl implements ParkMediaVideoService {

    private final ParkMediaVideoMapper mediaVideoMapper;

    @Override
    public PageResult<ParkMediaVideoVO> page(ParkMediaVideoQueryDTO query) {
        LambdaQueryWrapper<ParkMediaVideo> wrapper = buildWrapper(query);
        Page<ParkMediaVideo> page = mediaVideoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkMediaVideoVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkMediaVideoVO> listByPark(String parkCode) {
        return mediaVideoMapper.selectList(new LambdaQueryWrapper<ParkMediaVideo>()
                .eq(ParkMediaVideo::getParkCode, parkCode)
                .orderByAsc(ParkMediaVideo::getSortOrder)
                .orderByAsc(ParkMediaVideo::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkMediaVideoVO getDetail(Long id) {
        return toVO(requireVideo(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkMediaVideoCreateDTO dto) {
        Long count = mediaVideoMapper.selectCount(new LambdaQueryWrapper<ParkMediaVideo>()
                .eq(ParkMediaVideo::getParkCode, dto.getParkCode())
                .eq(ParkMediaVideo::getVideoUrl, dto.getVideoUrl()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "视频URL已存在: " + dto.getVideoUrl());
        }

        ParkMediaVideo entity = new ParkMediaVideo();
        entity.setParkCode(dto.getParkCode());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setCoverUrl(dto.getCoverUrl());
        entity.setVideoName(dto.getVideoName());
        entity.setVideoType(dto.getVideoType());
        entity.setVideoDescription(dto.getVideoDescription());
        entity.setDuration(dto.getDuration());
        entity.setFileSize(dto.getFileSize());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        mediaVideoMapper.insert(entity);
        log.info("创建机构视频成功: parkCode={}, id={}", dto.getParkCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkMediaVideoUpdateDTO dto) {
        ParkMediaVideo existing = requireVideo(id);
        ParkMediaVideo update = new ParkMediaVideo();
        update.setId(existing.getId());
        if (dto.getVideoUrl() != null && !dto.getVideoUrl().equals(existing.getVideoUrl())) {
            Long count = mediaVideoMapper.selectCount(new LambdaQueryWrapper<ParkMediaVideo>()
                    .eq(ParkMediaVideo::getParkCode, existing.getParkCode())
                    .eq(ParkMediaVideo::getVideoUrl, dto.getVideoUrl())
                    .ne(ParkMediaVideo::getId, id));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "视频URL已存在: " + dto.getVideoUrl());
            }
            update.setVideoUrl(dto.getVideoUrl());
        }
        if (dto.getCoverUrl() != null) update.setCoverUrl(dto.getCoverUrl());
        if (dto.getVideoName() != null) update.setVideoName(dto.getVideoName());
        if (dto.getVideoType() != null) update.setVideoType(dto.getVideoType());
        if (dto.getVideoDescription() != null) update.setVideoDescription(dto.getVideoDescription());
        if (dto.getDuration() != null) update.setDuration(dto.getDuration());
        if (dto.getFileSize() != null) update.setFileSize(dto.getFileSize());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        mediaVideoMapper.updateById(update);
        log.info("更新机构视频成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkMediaVideo existing = requireVideo(id);
        mediaVideoMapper.deleteById(existing.getId());
        log.info("删除机构视频成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkMediaVideo> buildWrapper(ParkMediaVideoQueryDTO query) {
        LambdaQueryWrapper<ParkMediaVideo> wrapper = new LambdaQueryWrapper<ParkMediaVideo>()
                .orderByAsc(ParkMediaVideo::getSortOrder)
                .orderByAsc(ParkMediaVideo::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkMediaVideo::getParkCode, query.getParkCode());
        }
        if (query.getVideoType() != null) {
            wrapper.eq(ParkMediaVideo::getVideoType, query.getVideoType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkMediaVideo::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkMediaVideo requireVideo(Long id) {
        ParkMediaVideo entity = mediaVideoMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构视频不存在: id=" + id);
        }
        return entity;
    }

    private ParkMediaVideoVO toVO(ParkMediaVideo entity) {
        ParkMediaVideoVO vo = new ParkMediaVideoVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setVideoUrl(entity.getVideoUrl());
        vo.setCoverUrl(entity.getCoverUrl());
        vo.setVideoName(entity.getVideoName());
        vo.setVideoType(entity.getVideoType());
        vo.setVideoDescription(entity.getVideoDescription());
        vo.setDuration(entity.getDuration());
        vo.setFileSize(entity.getFileSize());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
