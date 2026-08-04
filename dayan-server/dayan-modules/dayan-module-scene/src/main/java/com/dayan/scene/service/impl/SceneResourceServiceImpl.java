package com.dayan.scene.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneResourceCreateDTO;
import com.dayan.scene.dto.SceneResourceQueryDTO;
import com.dayan.scene.dto.SceneResourceUpdateDTO;
import com.dayan.scene.entity.SceneResource;
import com.dayan.scene.mapper.SceneResourceMapper;
import com.dayan.scene.service.SceneResourceService;
import com.dayan.scene.vo.SceneResourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 场景资源服务实现。
 *
 * <p>{@code scene_resource} 按 {@code sceneCode} 维度管理场景资源。
 *
 * <p><b>资源冲突检测（应用层）</b>：同 {@code sceneCode} 下，重复的资源标识
 * （{@code resourceName + resourceType}）视为冲突，create/update 时校验，
 * 重复抛 BusinessException("场景资源已存在")。
 *
 * <p><b>schema 限制说明</b>：当前 {@code scene_resource} 表无时间字段（{@code start_time}/{@code end_time}/{@code date}），
 * 也无 {@code resourceCode}/{@code resourceId} 业务编码，故无法做真正的"时间段重叠"检测
 * （区间重叠判定 {@code newStart<existEnd && newEnd>existStart} 需要起止时间）。
 * 本实现降级为"同 sceneCode 下 resourceName + resourceType 重复"判定。
 * 若后续表结构补充时间字段，应改为参考 {@code ButlerScheduleServiceImpl#checkOverlap} 的区间检测。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneResourceServiceImpl implements SceneResourceService {

    /** 默认状态：有效 */
    private static final int DEFAULT_STATUS = 1;

    private final SceneResourceMapper sceneResourceMapper;

    @Override
    public PageResult<SceneResourceVO> page(SceneResourceQueryDTO query) {
        LambdaQueryWrapper<SceneResource> wrapper = buildQueryWrapper(query);
        Page<SceneResource> page = sceneResourceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SceneResourceVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<SceneResourceVO> list(SceneResourceQueryDTO query) {
        LambdaQueryWrapper<SceneResource> wrapper = buildQueryWrapper(query);
        return sceneResourceMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public SceneResourceVO getDetail(Long id) {
        return toVO(requireResource(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SceneResourceCreateDTO dto) {
        // 冲突检测：同 sceneCode 下 resourceName + resourceType 重复
        checkConflict(dto.getSceneCode(), dto.getResourceName(), dto.getResourceType(), null);

        SceneResource entity = new SceneResource();
        entity.setSceneCode(dto.getSceneCode());
        entity.setResourceType(dto.getResourceType());
        entity.setResourceName(dto.getResourceName());
        entity.setResourceDescription(dto.getResourceDescription());
        entity.setQuantity(dto.getQuantity());
        entity.setUnit(dto.getUnit());
        entity.setUnitCost(dto.getUnitCost());
        entity.setIsProvided(dto.getIsProvided());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        sceneResourceMapper.insert(entity);
        log.info("创建场景资源成功: id={}, sceneCode={}, resourceName={}",
                entity.getId(), dto.getSceneCode(), dto.getResourceName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SceneResourceUpdateDTO dto) {
        SceneResource existing = requireResource(id);

        // 若改名或改类型，重新检测冲突
        String newName = dto.getResourceName() != null ? dto.getResourceName() : existing.getResourceName();
        Integer newType = dto.getResourceType() != null ? dto.getResourceType() : existing.getResourceType();
        if (dto.getResourceName() != null || dto.getResourceType() != null) {
            checkConflict(existing.getSceneCode(), newName, newType, id);
        }

        SceneResource update = new SceneResource();
        update.setId(existing.getId());
        if (dto.getResourceType() != null) update.setResourceType(dto.getResourceType());
        if (dto.getResourceName() != null) update.setResourceName(dto.getResourceName());
        if (dto.getResourceDescription() != null) update.setResourceDescription(dto.getResourceDescription());
        if (dto.getQuantity() != null) update.setQuantity(dto.getQuantity());
        if (dto.getUnit() != null) update.setUnit(dto.getUnit());
        if (dto.getUnitCost() != null) update.setUnitCost(dto.getUnitCost());
        if (dto.getIsProvided() != null) update.setIsProvided(dto.getIsProvided());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        sceneResourceMapper.updateById(update);
        log.info("更新场景资源成功: id={}, sceneCode={}", id, existing.getSceneCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SceneResource existing = requireResource(id);
        sceneResourceMapper.deleteById(existing.getId());
        log.info("删除场景资源成功: id={}, sceneCode={}", id, existing.getSceneCode());
    }

    // ====== 内部方法 ======

    /**
     * 资源冲突检测：同 sceneCode 下 resourceName + resourceType 重复判定。
     *
     * <p>schema 限制：本表无时间字段，无法做区间重叠检测，降级为标识唯一校验。
     *
     * @param sceneCode    场景编码
     * @param resourceName 资源名称
     * @param resourceType 资源类型（可空，为空时不参与匹配）
     * @param excludeId    更新时排除自身 id；新增时传 null
     */
    private void checkConflict(String sceneCode, String resourceName,
                               Integer resourceType, Long excludeId) {
        if (resourceName == null || resourceName.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SceneResource> wrapper = new LambdaQueryWrapper<SceneResource>()
                .eq(SceneResource::getSceneCode, sceneCode)
                .eq(SceneResource::getResourceName, resourceName);
        if (resourceType != null) {
            wrapper.eq(SceneResource::getResourceType, resourceType);
        }
        if (excludeId != null) {
            wrapper.ne(SceneResource::getId, excludeId);
        }
        Long count = sceneResourceMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "场景资源已存在（同场景下 resourceName+resourceType 冲突）: sceneCode="
                            + sceneCode + ", resourceName=" + resourceName);
        }
    }

    private LambdaQueryWrapper<SceneResource> buildQueryWrapper(SceneResourceQueryDTO query) {
        return new LambdaQueryWrapper<SceneResource>()
                .eq(query.getSceneCode() != null && !query.getSceneCode().isEmpty(),
                        SceneResource::getSceneCode, query.getSceneCode())
                .eq(query.getResourceType() != null,
                        SceneResource::getResourceType, query.getResourceType())
                .like(query.getResourceName() != null && !query.getResourceName().isEmpty(),
                        SceneResource::getResourceName, query.getResourceName())
                .eq(query.getIsProvided() != null,
                        SceneResource::getIsProvided, query.getIsProvided())
                .eq(query.getStatus() != null,
                        SceneResource::getStatus, query.getStatus())
                .orderByAsc(SceneResource::getSortOrder)
                .orderByAsc(SceneResource::getId);
    }

    private SceneResource requireResource(Long id) {
        SceneResource resource = sceneResourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景资源不存在: " + id);
        }
        return resource;
    }

    private SceneResourceVO toVO(SceneResource entity) {
        SceneResourceVO vo = new SceneResourceVO();
        vo.setId(entity.getId());
        vo.setSceneCode(entity.getSceneCode());
        vo.setResourceType(entity.getResourceType());
        vo.setResourceName(entity.getResourceName());
        vo.setResourceDescription(entity.getResourceDescription());
        vo.setQuantity(entity.getQuantity());
        vo.setUnit(entity.getUnit());
        vo.setUnitCost(entity.getUnitCost());
        vo.setIsProvided(entity.getIsProvided());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
