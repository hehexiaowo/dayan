package com.dayan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.system.dto.SystemAssetCreateDTO;
import com.dayan.system.dto.SystemAssetQueryDTO;
import com.dayan.system.dto.SystemAssetUpdateDTO;
import com.dayan.system.entity.SystemAsset;
import com.dayan.system.mapper.SystemAssetMapper;
import com.dayan.system.service.SystemAssetService;
import com.dayan.system.service.support.AssetRefMap;
import com.dayan.system.vo.SystemAssetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统素材仓库（system_asset）服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAssetServiceImpl implements SystemAssetService {

    /** 存储方式：本地 OSS 对象 */
    private static final int STORAGE_OSS = 1;
    /** 存储方式：外部链接 */
    private static final int STORAGE_EXTERNAL = 2;

    /** 类型1 默认值：平台素材 */
    private static final String REF_TYPE1_DEFAULT = "platform";
    /** 类型2 默认值：素材仓库直录 */
    private static final String REF_TYPE2_DEFAULT = "media_mgmt";

    private final SystemAssetMapper assetMapper;

    /** 引用校验用原生查询（跨表 count，不建 XML） */
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<SystemAssetVO> page(SystemAssetQueryDTO query) {
        LambdaQueryWrapper<SystemAsset> wrapper = buildWrapper(query);
        Page<SystemAsset> page = assetMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SystemAssetVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<SystemAssetVO> listByRef(String refType1, String refCode) {
        return assetMapper.selectList(refWrapper(refType1, refCode)
                .orderByAsc(SystemAsset::getAssetType)
                .orderByAsc(SystemAsset::getSortOrder)
                .orderByAsc(SystemAsset::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<SystemAssetVO> listByRefAndType(String refType1, String refCode, Integer assetType) {
        return assetMapper.selectList(refWrapper(refType1, refCode)
                .eq(SystemAsset::getAssetType, assetType)
                .orderByAsc(SystemAsset::getSortOrder)
                .orderByAsc(SystemAsset::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SystemAssetVO getDetail(Long id) {
        return toVO(requireAsset(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemAssetCreateDTO dto) {
        int storageType = dto.getStorageType() == null ? STORAGE_OSS : dto.getStorageType();
        validateExternalUrl(storageType, dto.getAssetUrl());
        SystemAsset entity = new SystemAsset();
        entity.setAssetType(dto.getAssetType());
        entity.setRefType1(defaultString(dto.getRefType1(), REF_TYPE1_DEFAULT));
        entity.setRefType2(defaultString(dto.getRefType2(), REF_TYPE2_DEFAULT));
        entity.setRefCode(blankToNull(dto.getRefCode()));
        entity.setStorageType(storageType);
        entity.setAssetUrl(dto.getAssetUrl());
        entity.setAssetName(dto.getAssetName());
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
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        assetMapper.insert(entity);
        log.info("创建素材成功: refType1={}, refCode={}, assetType={}, storageType={}, id={}",
                entity.getRefType1(), entity.getRefCode(), dto.getAssetType(), storageType, entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemAssetUpdateDTO dto) {
        SystemAsset existing = requireAsset(id);
        SystemAsset update = new SystemAsset();
        update.setId(existing.getId());
        if (dto.getRefType1() != null) update.setRefType1(blankToNull(dto.getRefType1()));
        if (dto.getRefType2() != null) update.setRefType2(blankToNull(dto.getRefType2()));
        if (dto.getRefCode() != null) update.setRefCode(blankToNull(dto.getRefCode()));
        if (dto.getStorageType() != null) update.setStorageType(dto.getStorageType());
        if (dto.getAssetUrl() != null) update.setAssetUrl(dto.getAssetUrl());
        if (dto.getAssetName() != null) update.setAssetName(dto.getAssetName());
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
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        // 存储方式与地址可能分别更新，按合并后的终态校验外链格式；
        // 仅当本次提交涉及这两个字段时校验（历史手填 http 地址的存量行可正常改其他字段）
        if (dto.getStorageType() != null || dto.getAssetUrl() != null) {
            Integer finalStorage = update.getStorageType() != null ? update.getStorageType() : existing.getStorageType();
            String finalUrl = update.getAssetUrl() != null ? update.getAssetUrl() : existing.getAssetUrl();
            validateExternalUrl(finalStorage, finalUrl);
        }
        assetMapper.updateById(update);
        log.info("更新素材成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SystemAsset existing = requireAsset(id);
        List<String> refs = findReferences(existing.getAssetUrl());
        if (!refs.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "素材被以下位置引用，无法删除：" + String.join("、", refs));
        }
        assetMapper.deleteById(existing.getId());
        log.info("删除素材成功: id={}", id);
    }

    /** 按 AssetRefMap 逐表反查引用；返回命中项的展示名列表 */
    private List<String> findReferences(String assetUrl) {
        if (assetUrl == null || assetUrl.isEmpty()) {
            return List.of();
        }
        List<String> hits = new ArrayList<>();
        for (AssetRefMap.RefCheck check : AssetRefMap.CHECKS) {
            String sql = switch (check.match()) {
                case EXACT -> "SELECT COUNT(*) FROM " + check.table()
                        + " WHERE " + check.column() + " = ? AND deleted = 0";
                case JSON_LIKE -> "SELECT COUNT(*) FROM " + check.table()
                        + " WHERE " + check.column() + " LIKE CONCAT('%\"', ?, '\"%') AND deleted = 0";
                case HTML_LIKE -> "SELECT COUNT(*) FROM " + check.table()
                        + " WHERE " + check.column() + " LIKE CONCAT('%', ?, '%') AND deleted = 0";
            };
            Long count = jdbcTemplate.queryForObject(sql, Long.class, assetUrl);
            if (count != null && count > 0) {
                hits.add(check.label());
            }
        }
        return hits;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerIfAbsent(String refType1, String refCode, Integer assetType, String assetUrl,
                                  String refType2, String assetName, Long fileSize) {
        String type1 = defaultString(refType1, REF_TYPE1_DEFAULT);
        String type2 = defaultString(refType2, REF_TYPE2_DEFAULT);
        String code = blankToNull(refCode);
        // 幂等查询：同 assetUrl + refType1 + refCode + refType2 已存在则返回已存 id
        LambdaQueryWrapper<SystemAsset> wrapper = new LambdaQueryWrapper<SystemAsset>()
                .eq(SystemAsset::getAssetUrl, assetUrl)
                .eq(SystemAsset::getRefType1, type1)
                .eq(SystemAsset::getRefType2, type2);
        if (code == null) {
            wrapper.isNull(SystemAsset::getRefCode);
        } else {
            wrapper.eq(SystemAsset::getRefCode, code);
        }
        wrapper.last("LIMIT 1");
        SystemAsset existing = assetMapper.selectOne(wrapper);
        if (existing != null) {
            return existing.getId();
        }
        // 不存在则创建（上传登记固定为本地 OSS）
        SystemAsset entity = new SystemAsset();
        entity.setRefType1(type1);
        entity.setRefCode(code);
        entity.setAssetType(assetType);
        entity.setRefType2(type2);
        entity.setStorageType(STORAGE_OSS);
        entity.setAssetUrl(assetUrl);
        entity.setAssetName(assetName);
        entity.setFileSize(fileSize);
        entity.setIsCover(0);
        entity.setSortOrder(0);
        entity.setStatus(1);
        assetMapper.insert(entity);
        log.info("登记素材成功: refType1={}, refCode={}, refType2={}, assetType={}, id={}",
                type1, code, type2, assetType, entity.getId());
        return entity.getId();
    }

    // ====== 内部方法 ======

    /** 外链素材的地址必须是完整 http(s) URL；OSS 素材地址不允许是外链（防混存） */
    private void validateExternalUrl(Integer storageType, String assetUrl) {
        if (storageType == null || assetUrl == null) {
            return;
        }
        boolean isHttp = assetUrl.startsWith("http://") || assetUrl.startsWith("https://");
        if (storageType == STORAGE_EXTERNAL && !isHttp) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "外链素材地址必须以 http:// 或 https:// 开头");
        }
        if (storageType == STORAGE_OSS && isHttp) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "本地OSS素材请填写文件key（以 http 开头的外部地址请选择「外链」存储方式）");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String defaultString(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    /** 分类三元组公共查询条件（refType1 必填语义，refCode 可空=该维度全部） */
    private LambdaQueryWrapper<SystemAsset> refWrapper(String refType1, String refCode) {
        LambdaQueryWrapper<SystemAsset> wrapper = new LambdaQueryWrapper<SystemAsset>()
                .eq(SystemAsset::getRefType1, defaultString(refType1, REF_TYPE1_DEFAULT));
        String code = blankToNull(refCode);
        if (code != null) {
            wrapper.eq(SystemAsset::getRefCode, code);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<SystemAsset> buildWrapper(SystemAssetQueryDTO query) {
        LambdaQueryWrapper<SystemAsset> wrapper = new LambdaQueryWrapper<SystemAsset>()
                .orderByAsc(SystemAsset::getSortOrder)
                .orderByAsc(SystemAsset::getId);
        if (query.getAssetType() != null) {
            wrapper.eq(SystemAsset::getAssetType, query.getAssetType());
        }
        if (query.getRefType1() != null && !query.getRefType1().isEmpty()) {
            wrapper.eq(SystemAsset::getRefType1, query.getRefType1());
        }
        if (query.getRefType2() != null && !query.getRefType2().isEmpty()) {
            wrapper.eq(SystemAsset::getRefType2, query.getRefType2());
        }
        if (query.getRefCode() != null && !query.getRefCode().isEmpty()) {
            wrapper.eq(SystemAsset::getRefCode, query.getRefCode());
        }
        if (query.getStorageType() != null) {
            wrapper.eq(SystemAsset::getStorageType, query.getStorageType());
        }
        if (query.getIsCover() != null) {
            wrapper.eq(SystemAsset::getIsCover, query.getIsCover());
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            String kw = query.getKeyword()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.and(w -> w.like(SystemAsset::getAssetName, kw)
                    .or().like(SystemAsset::getAssetUrl, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SystemAsset::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private SystemAsset requireAsset(Long id) {
        SystemAsset entity = assetMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "素材不存在: id=" + id);
        }
        return entity;
    }

    private SystemAssetVO toVO(SystemAsset entity) {
        SystemAssetVO vo = new SystemAssetVO();
        vo.setId(entity.getId());
        vo.setAssetType(entity.getAssetType());
        vo.setRefType1(entity.getRefType1());
        vo.setRefType2(entity.getRefType2());
        vo.setRefCode(entity.getRefCode());
        vo.setStorageType(entity.getStorageType());
        vo.setAssetUrl(entity.getAssetUrl());
        vo.setAssetName(entity.getAssetName());
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
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
