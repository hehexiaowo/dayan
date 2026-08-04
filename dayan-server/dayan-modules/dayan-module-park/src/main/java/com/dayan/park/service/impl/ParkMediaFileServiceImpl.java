package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkMediaFileCreateDTO;
import com.dayan.park.dto.ParkMediaFileQueryDTO;
import com.dayan.park.dto.ParkMediaFileUpdateDTO;
import com.dayan.park.entity.ParkMediaFile;
import com.dayan.park.mapper.ParkMediaFileMapper;
import com.dayan.park.service.ParkMediaFileService;
import com.dayan.park.vo.ParkMediaFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构文件（park_media_file）服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkMediaFileServiceImpl implements ParkMediaFileService {

    private final ParkMediaFileMapper mediaFileMapper;

    @Override
    public PageResult<ParkMediaFileVO> page(ParkMediaFileQueryDTO query) {
        LambdaQueryWrapper<ParkMediaFile> wrapper = buildWrapper(query);
        Page<ParkMediaFile> page = mediaFileMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkMediaFileVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkMediaFileVO> listByPark(String parkCode) {
        return mediaFileMapper.selectList(new LambdaQueryWrapper<ParkMediaFile>()
                .eq(ParkMediaFile::getParkCode, parkCode)
                .orderByAsc(ParkMediaFile::getSortOrder)
                .orderByAsc(ParkMediaFile::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkMediaFileVO getDetail(Long id) {
        return toVO(requireFile(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkMediaFileCreateDTO dto) {
        Long count = mediaFileMapper.selectCount(new LambdaQueryWrapper<ParkMediaFile>()
                .eq(ParkMediaFile::getParkCode, dto.getParkCode())
                .eq(ParkMediaFile::getFileUrl, dto.getFileUrl()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "文件URL已存在: " + dto.getFileUrl());
        }

        ParkMediaFile entity = new ParkMediaFile();
        entity.setParkCode(dto.getParkCode());
        entity.setFileUrl(dto.getFileUrl());
        entity.setFileName(dto.getFileName());
        entity.setFileType(dto.getFileType());
        entity.setFileFormat(dto.getFileFormat());
        entity.setFileSize(dto.getFileSize());
        entity.setFileDescription(dto.getFileDescription());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        mediaFileMapper.insert(entity);
        log.info("创建机构文件成功: parkCode={}, id={}", dto.getParkCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkMediaFileUpdateDTO dto) {
        ParkMediaFile existing = requireFile(id);
        ParkMediaFile update = new ParkMediaFile();
        update.setId(existing.getId());
        if (dto.getFileUrl() != null && !dto.getFileUrl().equals(existing.getFileUrl())) {
            Long count = mediaFileMapper.selectCount(new LambdaQueryWrapper<ParkMediaFile>()
                    .eq(ParkMediaFile::getParkCode, existing.getParkCode())
                    .eq(ParkMediaFile::getFileUrl, dto.getFileUrl())
                    .ne(ParkMediaFile::getId, id));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "文件URL已存在: " + dto.getFileUrl());
            }
            update.setFileUrl(dto.getFileUrl());
        }
        if (dto.getFileName() != null) update.setFileName(dto.getFileName());
        if (dto.getFileType() != null) update.setFileType(dto.getFileType());
        if (dto.getFileFormat() != null) update.setFileFormat(dto.getFileFormat());
        if (dto.getFileSize() != null) update.setFileSize(dto.getFileSize());
        if (dto.getFileDescription() != null) update.setFileDescription(dto.getFileDescription());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        mediaFileMapper.updateById(update);
        log.info("更新机构文件成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkMediaFile existing = requireFile(id);
        mediaFileMapper.deleteById(existing.getId());
        log.info("删除机构文件成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkMediaFile> buildWrapper(ParkMediaFileQueryDTO query) {
        LambdaQueryWrapper<ParkMediaFile> wrapper = new LambdaQueryWrapper<ParkMediaFile>()
                .orderByAsc(ParkMediaFile::getSortOrder)
                .orderByAsc(ParkMediaFile::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkMediaFile::getParkCode, query.getParkCode());
        }
        if (query.getFileType() != null) {
            wrapper.eq(ParkMediaFile::getFileType, query.getFileType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkMediaFile::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkMediaFile requireFile(Long id) {
        ParkMediaFile entity = mediaFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构文件不存在: id=" + id);
        }
        return entity;
    }

    private ParkMediaFileVO toVO(ParkMediaFile entity) {
        ParkMediaFileVO vo = new ParkMediaFileVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setFileUrl(entity.getFileUrl());
        vo.setFileName(entity.getFileName());
        vo.setFileType(entity.getFileType());
        vo.setFileFormat(entity.getFileFormat());
        vo.setFileSize(entity.getFileSize());
        vo.setFileDescription(entity.getFileDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
