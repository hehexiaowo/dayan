package com.dayan.scene.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneItemCreateDTO;
import com.dayan.scene.dto.SceneItemQueryDTO;
import com.dayan.scene.dto.SceneItemUpdateDTO;
import com.dayan.scene.entity.SceneItem;
import com.dayan.scene.mapper.SceneItemMapper;
import com.dayan.scene.service.SceneItemService;
import com.dayan.scene.vo.SceneItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 场景项目服务实现。
 *
 * <p>{@code scene_item} 按 {@code sceneCode} 维度管理，{@code itemCode} 同场景内唯一
 * （应用层校验，{@code sceneCode + itemCode} 联合唯一）。默认 status=1。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneItemServiceImpl implements SceneItemService {

    /** 默认状态：启用 */
    private static final int DEFAULT_STATUS = 1;

    private final SceneItemMapper sceneItemMapper;

    @Override
    public PageResult<SceneItemVO> page(SceneItemQueryDTO query) {
        LambdaQueryWrapper<SceneItem> wrapper = buildQueryWrapper(query);
        Page<SceneItem> page = sceneItemMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SceneItemVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<SceneItemVO> list(SceneItemQueryDTO query) {
        LambdaQueryWrapper<SceneItem> wrapper = buildQueryWrapper(query);
        return sceneItemMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public SceneItemVO getDetail(Long id) {
        return toVO(requireItem(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SceneItemCreateDTO dto) {
        // itemCode 同场景内唯一校验
        checkItemCodeUniqueInScene(dto.getSceneCode(), dto.getItemCode(), null);

        SceneItem entity = new SceneItem();
        entity.setSceneCode(dto.getSceneCode());
        entity.setItemCode(dto.getItemCode());
        entity.setItemName(dto.getItemName());
        entity.setItemType(dto.getItemType());
        entity.setItemDescription(dto.getItemDescription());
        entity.setDurationMinutes(dto.getDurationMinutes());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setIsRequired(dto.getIsRequired());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        sceneItemMapper.insert(entity);
        log.info("创建场景项目成功: id={}, sceneCode={}, itemCode={}",
                entity.getId(), dto.getSceneCode(), dto.getItemCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SceneItemUpdateDTO dto) {
        SceneItem existing = requireItem(id);
        SceneItem update = new SceneItem();
        update.setId(existing.getId());

        if (dto.getItemName() != null) update.setItemName(dto.getItemName());
        if (dto.getItemType() != null) update.setItemType(dto.getItemType());
        if (dto.getItemDescription() != null) update.setItemDescription(dto.getItemDescription());
        if (dto.getDurationMinutes() != null) update.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getIsRequired() != null) update.setIsRequired(dto.getIsRequired());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        sceneItemMapper.updateById(update);
        log.info("更新场景项目成功: id={}, sceneCode={}", id, existing.getSceneCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SceneItem existing = requireItem(id);
        sceneItemMapper.deleteById(existing.getId());
        log.info("删除场景项目成功: id={}, sceneCode={}", id, existing.getSceneCode());
    }

    // ====== 内部方法 ======

    /**
     * 校验同场景内 itemCode 唯一。
     *
     * @param sceneCode  场景编码
     * @param itemCode   项目编码
     * @param excludeId  更新时排除自身 id；新增时传 null
     */
    private void checkItemCodeUniqueInScene(String sceneCode, String itemCode, Long excludeId) {
        if (itemCode == null || itemCode.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SceneItem> wrapper = new LambdaQueryWrapper<SceneItem>()
                .eq(SceneItem::getSceneCode, sceneCode)
                .eq(SceneItem::getItemCode, itemCode);
        if (excludeId != null) {
            wrapper.ne(SceneItem::getId, excludeId);
        }
        Long count = sceneItemMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "场景内项目编码已存在: sceneCode=" + sceneCode + ", itemCode=" + itemCode);
        }
    }

    private LambdaQueryWrapper<SceneItem> buildQueryWrapper(SceneItemQueryDTO query) {
        return new LambdaQueryWrapper<SceneItem>()
                .eq(query.getSceneCode() != null && !query.getSceneCode().isEmpty(),
                        SceneItem::getSceneCode, query.getSceneCode())
                .eq(query.getItemCode() != null && !query.getItemCode().isEmpty(),
                        SceneItem::getItemCode, query.getItemCode())
                .like(query.getItemName() != null && !query.getItemName().isEmpty(),
                        SceneItem::getItemName, query.getItemName())
                .eq(query.getItemType() != null, SceneItem::getItemType, query.getItemType())
                .eq(query.getIsRequired() != null, SceneItem::getIsRequired, query.getIsRequired())
                .eq(query.getStatus() != null, SceneItem::getStatus, query.getStatus())
                .orderByAsc(SceneItem::getSortOrder)
                .orderByAsc(SceneItem::getId);
    }

    private SceneItem requireItem(Long id) {
        SceneItem item = sceneItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景项目不存在: " + id);
        }
        return item;
    }

    private SceneItemVO toVO(SceneItem entity) {
        SceneItemVO vo = new SceneItemVO();
        vo.setId(entity.getId());
        vo.setSceneCode(entity.getSceneCode());
        vo.setItemCode(entity.getItemCode());
        vo.setItemName(entity.getItemName());
        vo.setItemType(entity.getItemType());
        vo.setItemDescription(entity.getItemDescription());
        vo.setDurationMinutes(entity.getDurationMinutes());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIsRequired(entity.getIsRequired());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
