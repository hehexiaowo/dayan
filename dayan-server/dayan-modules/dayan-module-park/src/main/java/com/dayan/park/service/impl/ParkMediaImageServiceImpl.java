package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkMediaImageCreateDTO;
import com.dayan.park.dto.ParkMediaImageQueryDTO;
import com.dayan.park.dto.ParkMediaImageUpdateDTO;
import com.dayan.park.entity.ParkMediaImage;
import com.dayan.park.mapper.ParkMediaImageMapper;
import com.dayan.park.service.ParkMediaImageService;
import com.dayan.park.vo.ParkMediaImageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构图片（park_media_image）服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkMediaImageServiceImpl implements ParkMediaImageService {

    private final ParkMediaImageMapper mediaImageMapper;

    @Override
    public PageResult<ParkMediaImageVO> page(ParkMediaImageQueryDTO query) {
        LambdaQueryWrapper<ParkMediaImage> wrapper = buildWrapper(query);
        Page<ParkMediaImage> page = mediaImageMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkMediaImageVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkMediaImageVO> listByPark(String parkCode) {
        return mediaImageMapper.selectList(new LambdaQueryWrapper<ParkMediaImage>()
                .eq(ParkMediaImage::getParkCode, parkCode)
                .orderByAsc(ParkMediaImage::getSortOrder)
                .orderByAsc(ParkMediaImage::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkMediaImageVO getDetail(Long id) {
        return toVO(requireImage(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkMediaImageCreateDTO dto) {
        // URL 唯一校验（同 parkCode 下）
        Long count = mediaImageMapper.selectCount(new LambdaQueryWrapper<ParkMediaImage>()
                .eq(ParkMediaImage::getParkCode, dto.getParkCode())
                .eq(ParkMediaImage::getImageUrl, dto.getImageUrl()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "图片URL已存在: " + dto.getImageUrl());
        }

        ParkMediaImage entity = new ParkMediaImage();
        entity.setParkCode(dto.getParkCode());
        entity.setImageUrl(dto.getImageUrl());
        entity.setImageName(dto.getImageName());
        entity.setImageType(dto.getImageType());
        entity.setImageDescription(dto.getImageDescription());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setFileSize(dto.getFileSize());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setIsCover(dto.getIsCover() == null ? 0 : dto.getIsCover());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        mediaImageMapper.insert(entity);
        log.info("创建机构图片成功: parkCode={}, id={}", dto.getParkCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkMediaImageUpdateDTO dto) {
        ParkMediaImage existing = requireImage(id);
        ParkMediaImage update = new ParkMediaImage();
        update.setId(existing.getId());
        if (dto.getImageUrl() != null) {
            if (!dto.getImageUrl().equals(existing.getImageUrl())) {
                Long count = mediaImageMapper.selectCount(new LambdaQueryWrapper<ParkMediaImage>()
                        .eq(ParkMediaImage::getParkCode, existing.getParkCode())
                        .eq(ParkMediaImage::getImageUrl, dto.getImageUrl())
                        .ne(ParkMediaImage::getId, id));
                if (count != null && count > 0) {
                    throw new BusinessException(ErrorCode.BUSINESS, "图片URL已存在: " + dto.getImageUrl());
                }
            }
            update.setImageUrl(dto.getImageUrl());
        }
        if (dto.getImageName() != null) update.setImageName(dto.getImageName());
        if (dto.getImageType() != null) update.setImageType(dto.getImageType());
        if (dto.getImageDescription() != null) update.setImageDescription(dto.getImageDescription());
        if (dto.getWidth() != null) update.setWidth(dto.getWidth());
        if (dto.getHeight() != null) update.setHeight(dto.getHeight());
        if (dto.getFileSize() != null) update.setFileSize(dto.getFileSize());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getIsCover() != null) update.setIsCover(dto.getIsCover());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        mediaImageMapper.updateById(update);
        log.info("更新机构图片成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkMediaImage existing = requireImage(id);
        mediaImageMapper.deleteById(existing.getId());
        log.info("删除机构图片成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkMediaImage> buildWrapper(ParkMediaImageQueryDTO query) {
        LambdaQueryWrapper<ParkMediaImage> wrapper = new LambdaQueryWrapper<ParkMediaImage>()
                .orderByAsc(ParkMediaImage::getSortOrder)
                .orderByAsc(ParkMediaImage::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkMediaImage::getParkCode, query.getParkCode());
        }
        if (query.getImageType() != null) {
            wrapper.eq(ParkMediaImage::getImageType, query.getImageType());
        }
        if (query.getIsCover() != null) {
            wrapper.eq(ParkMediaImage::getIsCover, query.getIsCover());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkMediaImage::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkMediaImage requireImage(Long id) {
        ParkMediaImage entity = mediaImageMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构图片不存在: id=" + id);
        }
        return entity;
    }

    private ParkMediaImageVO toVO(ParkMediaImage entity) {
        ParkMediaImageVO vo = new ParkMediaImageVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setImageUrl(entity.getImageUrl());
        vo.setImageName(entity.getImageName());
        vo.setImageType(entity.getImageType());
        vo.setImageDescription(entity.getImageDescription());
        vo.setWidth(entity.getWidth());
        vo.setHeight(entity.getHeight());
        vo.setFileSize(entity.getFileSize());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIsCover(entity.getIsCover());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
