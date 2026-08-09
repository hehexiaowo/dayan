package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkAssetCreateDTO;
import com.dayan.park.dto.ParkAssetQueryDTO;
import com.dayan.park.dto.ParkAssetUpdateDTO;
import com.dayan.park.entity.ParkAsset;
import com.dayan.park.mapper.ParkAssetMapper;
import com.dayan.park.service.ParkAssetService;
import com.dayan.park.vo.ParkAssetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构素材库（park_asset）服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkAssetServiceImpl implements ParkAssetService {

    private final ParkAssetMapper assetMapper;

    @Override
    public PageResult<ParkAssetVO> page(ParkAssetQueryDTO query) {
        LambdaQueryWrapper<ParkAsset> wrapper = buildWrapper(query);
        Page<ParkAsset> page = assetMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkAssetVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkAssetVO> listByPark(String parkCode) {
        return assetMapper.selectList(new LambdaQueryWrapper<ParkAsset>()
                .eq(ParkAsset::getParkCode, parkCode)
                .orderByAsc(ParkAsset::getAssetType)
                .orderByAsc(ParkAsset::getSortOrder)
                .orderByAsc(ParkAsset::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ParkAssetVO> listByParkAndType(String parkCode, Integer assetType) {
        return assetMapper.selectList(new LambdaQueryWrapper<ParkAsset>()
                .eq(ParkAsset::getParkCode, parkCode)
                .eq(ParkAsset::getAssetType, assetType)
                .orderByAsc(ParkAsset::getSortOrder)
                .orderByAsc(ParkAsset::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkAssetVO getDetail(Long id) {
        return toVO(requireAsset(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkAssetCreateDTO dto) {
        ParkAsset entity = new ParkAsset();
        entity.setParkCode(dto.getParkCode());
        entity.setAssetType(dto.getAssetType());
        entity.setAssetUrl(dto.getAssetUrl());
        entity.setAssetName(dto.getAssetName());
        entity.setAssetCategory(dto.getAssetCategory());
        entity.setDescription(dto.getDescription());
        entity.setFileSize(dto.getFileSize());
        entity.setWidth(dto.getWidth());
        entity.setHeight(dto.getHeight());
        entity.setIsCover(dto.getIsCover() == null ? 0 : dto.getIsCover());
        entity.setCoverUrl(dto.getCoverUrl());
        entity.setDuration(dto.getDuration());
        entity.setFileFormat(dto.getFileFormat());
        entity.setVrProvider(dto.getVrProvider());
        entity.setThumbnailUrl(dto.getThumbnailUrl());
        entity.setSourceType(dto.getSourceType() == null ? "media_mgmt" : dto.getSourceType());
        entity.setSourceRefCode(dto.getSourceRefCode());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        assetMapper.insert(entity);
        log.info("创建素材成功: parkCode={}, assetType={}, id={}", dto.getParkCode(), dto.getAssetType(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkAssetUpdateDTO dto) {
        ParkAsset existing = requireAsset(id);
        ParkAsset update = new ParkAsset();
        update.setId(existing.getId());
        if (dto.getAssetUrl() != null) update.setAssetUrl(dto.getAssetUrl());
        if (dto.getAssetName() != null) update.setAssetName(dto.getAssetName());
        if (dto.getAssetCategory() != null) update.setAssetCategory(dto.getAssetCategory());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getFileSize() != null) update.setFileSize(dto.getFileSize());
        if (dto.getWidth() != null) update.setWidth(dto.getWidth());
        if (dto.getHeight() != null) update.setHeight(dto.getHeight());
        if (dto.getIsCover() != null) update.setIsCover(dto.getIsCover());
        if (dto.getCoverUrl() != null) update.setCoverUrl(dto.getCoverUrl());
        if (dto.getDuration() != null) update.setDuration(dto.getDuration());
        if (dto.getFileFormat() != null) update.setFileFormat(dto.getFileFormat());
        if (dto.getVrProvider() != null) update.setVrProvider(dto.getVrProvider());
        if (dto.getThumbnailUrl() != null) update.setThumbnailUrl(dto.getThumbnailUrl());
        if (dto.getSourceType() != null) update.setSourceType(dto.getSourceType());
        if (dto.getSourceRefCode() != null) update.setSourceRefCode(dto.getSourceRefCode());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        assetMapper.updateById(update);
        log.info("更新素材成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkAsset existing = requireAsset(id);
        assetMapper.deleteById(existing.getId());
        log.info("删除素材成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerIfAbsent(String parkCode, Integer assetType, String assetUrl,
                                  String sourceType, String sourceRefCode,
                                  String assetName, Long fileSize) {
        String srcType = sourceType == null ? "media_mgmt" : sourceType;
        // 幂等查询：同 parkCode + assetUrl + sourceType + sourceRefCode 已存在则返回已存 id
        LambdaQueryWrapper<ParkAsset> wrapper = new LambdaQueryWrapper<ParkAsset>()
                .eq(ParkAsset::getParkCode, parkCode)
                .eq(ParkAsset::getAssetUrl, assetUrl)
                .eq(ParkAsset::getSourceType, srcType);
        if (sourceRefCode != null && !sourceRefCode.isEmpty()) {
            wrapper.eq(ParkAsset::getSourceRefCode, sourceRefCode);
        } else {
            wrapper.isNull(ParkAsset::getSourceRefCode);
        }
        wrapper.last("LIMIT 1");
        ParkAsset existing = assetMapper.selectOne(wrapper);
        if (existing != null) {
            return existing.getId();
        }
        // 不存在则创建
        ParkAsset entity = new ParkAsset();
        entity.setParkCode(parkCode);
        entity.setAssetType(assetType);
        entity.setAssetUrl(assetUrl);
        entity.setAssetName(assetName);
        entity.setFileSize(fileSize);
        entity.setSourceType(srcType);
        entity.setSourceRefCode((sourceRefCode != null && sourceRefCode.isEmpty()) ? null : sourceRefCode);
        entity.setIsCover(0);
        entity.setSortOrder(0);
        entity.setStatus(1);
        assetMapper.insert(entity);
        log.info("注册素材成功: parkCode={}, assetType={}, sourceType={}, sourceRefCode={}, id={}",
                parkCode, assetType, srcType, sourceRefCode, entity.getId());
        return entity.getId();
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkAsset> buildWrapper(ParkAssetQueryDTO query) {
        LambdaQueryWrapper<ParkAsset> wrapper = new LambdaQueryWrapper<ParkAsset>()
                .orderByAsc(ParkAsset::getSortOrder)
                .orderByAsc(ParkAsset::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkAsset::getParkCode, query.getParkCode());
        }
        if (query.getAssetType() != null) {
            wrapper.eq(ParkAsset::getAssetType, query.getAssetType());
        }
        if (query.getAssetCategory() != null) {
            wrapper.eq(ParkAsset::getAssetCategory, query.getAssetCategory());
        }
        if (query.getIsCover() != null) {
            wrapper.eq(ParkAsset::getIsCover, query.getIsCover());
        }
        if (query.getSourceType() != null && !query.getSourceType().isEmpty()) {
            wrapper.eq(ParkAsset::getSourceType, query.getSourceType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkAsset::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkAsset requireAsset(Long id) {
        ParkAsset entity = assetMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "素材不存在: id=" + id);
        }
        return entity;
    }

    private ParkAssetVO toVO(ParkAsset entity) {
        ParkAssetVO vo = new ParkAssetVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setAssetType(entity.getAssetType());
        vo.setAssetUrl(entity.getAssetUrl());
        vo.setAssetName(entity.getAssetName());
        vo.setAssetCategory(entity.getAssetCategory());
        vo.setDescription(entity.getDescription());
        vo.setFileSize(entity.getFileSize());
        vo.setWidth(entity.getWidth());
        vo.setHeight(entity.getHeight());
        vo.setIsCover(entity.getIsCover());
        vo.setCoverUrl(entity.getCoverUrl());
        vo.setDuration(entity.getDuration());
        vo.setFileFormat(entity.getFileFormat());
        vo.setVrProvider(entity.getVrProvider());
        vo.setThumbnailUrl(entity.getThumbnailUrl());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceRefCode(entity.getSourceRefCode());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
