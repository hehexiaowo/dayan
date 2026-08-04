package com.dayan.scene.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.scene.dto.SceneInfoAuditDTO;
import com.dayan.scene.dto.SceneInfoCreateDTO;
import com.dayan.scene.dto.SceneInfoQueryDTO;
import com.dayan.scene.dto.SceneInfoUpdateDTO;
import com.dayan.scene.entity.SceneInfo;
import com.dayan.scene.enums.SceneEvent;
import com.dayan.scene.mapper.SceneInfoMapper;
import com.dayan.scene.service.SceneInfoService;
import com.dayan.scene.vo.SceneInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 场景信息服务实现。
 *
 * <p>审核流（sceneStatus 状态流转，经 SCENE_SM 状态机校验）：
 * <ul>
 *   <li>{@code create}：初始 0（草稿）</li>
 *   <li>{@code submit}：置 audit_status=0（待审），不改 sceneStatus</li>
 *   <li>{@code audit}：置 audit_status=1 通过 / 2 驳回，不改 sceneStatus</li>
 *   <li>{@code shelves}：0 草稿 → 1 已上架（{@link SceneEvent#SHELVES}，要求 audit_status=1）</li>
 *   <li>{@code offshelves}：1 已上架 → 2 已下架（{@link SceneEvent#OFFSHELVES}）</li>
 *   <li>{@code reshelves}：2 已下架 → 1 已上架（{@link SceneEvent#RESHELVES}）</li>
 * </ul>
 *
 * <p>{@code scene_info} 平台共享表（{@code DayanTenantHandler} 忽略前缀），Admin 全局管理。
 * 编码生成：{@code "SC" + String.format("%05d", sequenceProvider.next("code:seq:SC:0"))}，
 * 全表唯一；{@code sceneName} 全表唯一；{@code parkCode} 弱校验（不跨模块查存在性）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneInfoServiceImpl implements SceneInfoService {

    /** 场景编码前缀 */
    private static final String CODE_PREFIX = "SC";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:SC:0";

    private final SceneInfoMapper sceneInfoMapper;
    private final SequenceProvider sequenceProvider;
    private final StateMachineEngine stateMachineEngine;

    @Override
    public PageResult<SceneInfoVO> page(SceneInfoQueryDTO query) {
        LambdaQueryWrapper<SceneInfo> wrapper = buildQueryWrapper(query);
        Page<SceneInfo> page = sceneInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SceneInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<SceneInfoVO> list(SceneInfoQueryDTO query) {
        LambdaQueryWrapper<SceneInfo> wrapper = buildQueryWrapper(query);
        return sceneInfoMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public SceneInfoVO getDetail(String sceneCode) {
        return toVO(requireScene(sceneCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SceneInfoCreateDTO dto) {
        // sceneName 全表唯一校验
        checkSceneNameUnique(dto.getSceneName(), null);

        String sceneCode = generateSceneCode();

        SceneInfo entity = new SceneInfo();
        entity.setSceneCode(sceneCode);
        entity.setSceneName(dto.getSceneName());
        entity.setSceneType(dto.getSceneType());
        entity.setParkCode(dto.getParkCode());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setAddress(dto.getAddress());
        entity.setSceneDescription(dto.getSceneDescription());
        entity.setCoverImage(dto.getCoverImage());
        entity.setImageUrls(dto.getImageUrls());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setCapacity(dto.getCapacity());
        entity.setDurationHours(dto.getDurationHours());
        entity.setTargetAudience(dto.getTargetAudience());
        entity.setHighlight(dto.getHighlight());
        entity.setNotice(dto.getNotice());
        entity.setMinPerson(dto.getMinPerson());
        entity.setMaxPerson(dto.getMaxPerson());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setPriceUnit(dto.getPriceUnit());
        entity.setIsFree(dto.getIsFree());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setViewCount(0);
        entity.setBookCount(0);
        entity.setSceneStatus(dto.getSceneStatus() == null ? SceneEvent.STATUS_DRAFT : dto.getSceneStatus());
        entity.setAuditStatus(dto.getAuditStatus() == null ? SceneEvent.AUDIT_PENDING : dto.getAuditStatus());
        entity.setRemark(dto.getRemark());

        sceneInfoMapper.insert(entity);
        log.info("创建场景成功: sceneCode={}, sceneName={}", sceneCode, dto.getSceneName());
        return sceneCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String sceneCode, SceneInfoUpdateDTO dto) {
        SceneInfo existing = requireScene(sceneCode);
        SceneInfo update = new SceneInfo();
        update.setId(existing.getId());

        if (dto.getSceneName() != null) {
            checkSceneNameUnique(dto.getSceneName(), sceneCode);
            update.setSceneName(dto.getSceneName());
        }
        if (dto.getSceneType() != null) update.setSceneType(dto.getSceneType());
        if (dto.getParkCode() != null) update.setParkCode(dto.getParkCode());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getSceneDescription() != null) update.setSceneDescription(dto.getSceneDescription());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getImageUrls() != null) update.setImageUrls(dto.getImageUrls());
        if (dto.getVideoUrl() != null) update.setVideoUrl(dto.getVideoUrl());
        if (dto.getCapacity() != null) update.setCapacity(dto.getCapacity());
        if (dto.getDurationHours() != null) update.setDurationHours(dto.getDurationHours());
        if (dto.getTargetAudience() != null) update.setTargetAudience(dto.getTargetAudience());
        if (dto.getHighlight() != null) update.setHighlight(dto.getHighlight());
        if (dto.getNotice() != null) update.setNotice(dto.getNotice());
        if (dto.getMinPerson() != null) update.setMinPerson(dto.getMinPerson());
        if (dto.getMaxPerson() != null) update.setMaxPerson(dto.getMaxPerson());
        if (dto.getOriginalPrice() != null) update.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getSalePrice() != null) update.setSalePrice(dto.getSalePrice());
        if (dto.getPriceUnit() != null) update.setPriceUnit(dto.getPriceUnit());
        if (dto.getIsFree() != null) update.setIsFree(dto.getIsFree());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getSceneStatus() != null) update.setSceneStatus(dto.getSceneStatus());
        if (dto.getAuditStatus() != null) update.setAuditStatus(dto.getAuditStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        sceneInfoMapper.updateById(update);
        log.info("更新场景成功: sceneCode={}", sceneCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String sceneCode) {
        SceneInfo existing = requireScene(sceneCode);
        sceneInfoMapper.deleteById(existing.getId());
        log.info("删除场景成功: sceneCode={}", sceneCode);
    }

    /** 提交审核（草稿态提交，audit_status 置为待审） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(String sceneCode) {
        SceneInfo existing = requireScene(sceneCode);
        SceneInfo update = new SceneInfo();
        update.setId(existing.getId());
        update.setAuditStatus(SceneEvent.AUDIT_PENDING);
        sceneInfoMapper.updateById(update);
        log.info("场景提交审核: sceneCode={}", sceneCode);
    }

    /** 审核（audit_status → 1通过 / 2驳回） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(SceneInfoAuditDTO dto) {
        SceneInfo existing = requireScene(dto.getSceneCode());
        Integer auditStatus = dto.getAuditStatus();
        if (auditStatus == null || (auditStatus != SceneEvent.AUDIT_PASS && auditStatus != SceneEvent.AUDIT_REJECT)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核状态非法（仅支持 1=通过 / 2=驳回）");
        }
        SceneInfo update = new SceneInfo();
        update.setId(existing.getId());
        update.setAuditStatus(auditStatus);
        sceneInfoMapper.updateById(update);
        log.info("场景审核完成: sceneCode={}, auditStatus={}", dto.getSceneCode(), auditStatus);
    }

    /** 上架（scene_status 0→1，要求 audit_status=1 通过） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shelves(String sceneCode) {
        SceneInfo existing = requireScene(sceneCode);
        if (existing.getAuditStatus() == null || existing.getAuditStatus() != SceneEvent.AUDIT_PASS) {
            throw new BusinessException(ErrorCode.BUSINESS, "场景未审核通过，不可上架: " + sceneCode);
        }
        int from = existing.getSceneStatus() == null ? SceneEvent.STATUS_DRAFT : existing.getSceneStatus();
        int to = stateMachineEngine.transition(SceneEvent.DOMAIN, from, SceneEvent.SHELVES);
        SceneInfo update = new SceneInfo();
        update.setId(existing.getId());
        update.setSceneStatus(to);
        sceneInfoMapper.updateById(update);
        log.info("场景上架: sceneCode={}", sceneCode);
    }

    /** 下架（scene_status 1→2） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offshelves(String sceneCode) {
        SceneInfo existing = requireScene(sceneCode);
        int from = existing.getSceneStatus() == null ? SceneEvent.STATUS_DRAFT : existing.getSceneStatus();
        int to = stateMachineEngine.transition(SceneEvent.DOMAIN, from, SceneEvent.OFFSHELVES);
        SceneInfo update = new SceneInfo();
        update.setId(existing.getId());
        update.setSceneStatus(to);
        sceneInfoMapper.updateById(update);
        log.info("场景下架: sceneCode={}", sceneCode);
    }

    /** 重新上架（scene_status 2→1） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reshelves(String sceneCode) {
        SceneInfo existing = requireScene(sceneCode);
        int from = existing.getSceneStatus() == null ? SceneEvent.STATUS_DRAFT : existing.getSceneStatus();
        int to = stateMachineEngine.transition(SceneEvent.DOMAIN, from, SceneEvent.RESHELVES);
        SceneInfo update = new SceneInfo();
        update.setId(existing.getId());
        update.setSceneStatus(to);
        sceneInfoMapper.updateById(update);
        log.info("场景重新上架: sceneCode={}", sceneCode);
    }

    // ====== 内部方法 ======

    /**
     * sceneName 全表唯一校验。
     *
     * @param sceneName     待校验名称
     * @param excludeScene  更新时排除自身的 sceneCode；新增时传 null
     */
    private void checkSceneNameUnique(String sceneName, String excludeScene) {
        if (sceneName == null || sceneName.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SceneInfo> wrapper = new LambdaQueryWrapper<SceneInfo>()
                .eq(SceneInfo::getSceneName, sceneName);
        if (excludeScene != null && !excludeScene.isEmpty()) {
            wrapper.ne(SceneInfo::getSceneCode, excludeScene);
        }
        Long count = sceneInfoMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "场景名称已存在: " + sceneName);
        }
    }

    private LambdaQueryWrapper<SceneInfo> buildQueryWrapper(SceneInfoQueryDTO query) {
        return new LambdaQueryWrapper<SceneInfo>()
                .eq(query.getSceneCode() != null && !query.getSceneCode().isEmpty(),
                        SceneInfo::getSceneCode, query.getSceneCode())
                .like(query.getSceneName() != null && !query.getSceneName().isEmpty(),
                        SceneInfo::getSceneName, query.getSceneName())
                .eq(query.getSceneType() != null, SceneInfo::getSceneType, query.getSceneType())
                .eq(query.getParkCode() != null && !query.getParkCode().isEmpty(),
                        SceneInfo::getParkCode, query.getParkCode())
                .eq(query.getSceneStatus() != null, SceneInfo::getSceneStatus, query.getSceneStatus())
                .eq(query.getAuditStatus() != null, SceneInfo::getAuditStatus, query.getAuditStatus())
                .orderByDesc(SceneInfo::getCreatedAt);
    }

    private SceneInfo requireScene(String sceneCode) {
        SceneInfo scene = sceneInfoMapper.selectOne(new LambdaQueryWrapper<SceneInfo>()
                .eq(SceneInfo::getSceneCode, sceneCode)
                .last("LIMIT 1"));
        if (scene == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景不存在: " + sceneCode);
        }
        return scene;
    }

    /** 生成场景编码：SC + 5 位序列 */
    private String generateSceneCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private SceneInfoVO toVO(SceneInfo entity) {
        SceneInfoVO vo = new SceneInfoVO();
        vo.setId(entity.getId());
        vo.setSceneCode(entity.getSceneCode());
        vo.setSceneName(entity.getSceneName());
        vo.setSceneType(entity.getSceneType());
        vo.setParkCode(entity.getParkCode());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setAddress(entity.getAddress());
        vo.setSceneDescription(entity.getSceneDescription());
        vo.setCoverImage(entity.getCoverImage());
        vo.setImageUrls(entity.getImageUrls());
        vo.setVideoUrl(entity.getVideoUrl());
        vo.setCapacity(entity.getCapacity());
        vo.setDurationHours(entity.getDurationHours());
        vo.setTargetAudience(entity.getTargetAudience());
        vo.setHighlight(entity.getHighlight());
        vo.setNotice(entity.getNotice());
        vo.setMinPerson(entity.getMinPerson());
        vo.setMaxPerson(entity.getMaxPerson());
        vo.setOriginalPrice(entity.getOriginalPrice());
        vo.setSalePrice(entity.getSalePrice());
        vo.setPriceUnit(entity.getPriceUnit());
        vo.setIsFree(entity.getIsFree());
        vo.setSortOrder(entity.getSortOrder());
        vo.setViewCount(entity.getViewCount());
        vo.setBookCount(entity.getBookCount());
        vo.setSceneStatus(entity.getSceneStatus());
        vo.setAuditStatus(entity.getAuditStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
