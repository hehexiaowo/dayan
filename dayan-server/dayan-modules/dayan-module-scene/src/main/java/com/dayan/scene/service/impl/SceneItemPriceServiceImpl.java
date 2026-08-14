package com.dayan.scene.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneItemPriceCreateDTO;
import com.dayan.scene.dto.SceneItemPriceQueryDTO;
import com.dayan.scene.dto.SceneItemPriceUpdateDTO;
import com.dayan.scene.entity.SceneItemPrice;
import com.dayan.scene.mapper.SceneItemPriceMapper;
import com.dayan.scene.service.SceneItemPriceService;
import com.dayan.scene.vo.SceneItemPriceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 场景项目定价服务实现。
 *
 * <p>{@code scene_item_price} 按 {@code sceneCode + sceneItemCode} 维度管理定价，
 * {@code channelPrice} 字段承载渠道差异化定价。
 *
 * <p>校验：若 {@code effectiveDate}/{@code expireDate} 同时提供，则 effective 必须 ≤ expire。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneItemPriceServiceImpl implements SceneItemPriceService {

    /** 默认状态：有效 */
    private static final int DEFAULT_STATUS = 1;

    private final SceneItemPriceMapper sceneItemPriceMapper;

    @Override
    public PageResult<SceneItemPriceVO> page(SceneItemPriceQueryDTO query) {
        LambdaQueryWrapper<SceneItemPrice> wrapper = buildQueryWrapper(query);
        Page<SceneItemPrice> page = sceneItemPriceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SceneItemPriceVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<SceneItemPriceVO> list(SceneItemPriceQueryDTO query) {
        LambdaQueryWrapper<SceneItemPrice> wrapper = buildQueryWrapper(query);
        return sceneItemPriceMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public SceneItemPriceVO getDetail(Long id) {
        return toVO(requirePrice(id));
    }

    @Override
    public BigDecimal getCurrentPersonPrice(String sceneCode, LocalDate activeOn) {
        SceneItemPrice price = sceneItemPriceMapper.selectList(new LambdaQueryWrapper<SceneItemPrice>()
                .eq(SceneItemPrice::getSceneCode, sceneCode)
                .eq(SceneItemPrice::getPriceType, 1)
                .eq(SceneItemPrice::getStatus, 1)
                .and(w -> w.isNull(SceneItemPrice::getEffectiveDate)
                        .or().le(SceneItemPrice::getEffectiveDate, activeOn))
                .and(w -> w.isNull(SceneItemPrice::getExpireDate)
                        .or().ge(SceneItemPrice::getExpireDate, activeOn))
                .orderByDesc(SceneItemPrice::getId)
                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        if (price == null) {
            return null;
        }
        return price.getChannelPrice() != null ? price.getChannelPrice() : price.getSalePrice();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SceneItemPriceCreateDTO dto) {
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());

        SceneItemPrice entity = new SceneItemPrice();
        entity.setSceneCode(dto.getSceneCode());
        entity.setSceneItemCode(dto.getSceneItemCode());
        entity.setPriceType(dto.getPriceType());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setChannelPrice(dto.getChannelPrice());
        entity.setPriceDescription(dto.getPriceDescription());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpireDate(dto.getExpireDate());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        sceneItemPriceMapper.insert(entity);
        log.info("创建场景项目定价成功: id={}, sceneCode={}, sceneItemCode={}",
                entity.getId(), dto.getSceneCode(), dto.getSceneItemCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SceneItemPriceUpdateDTO dto) {
        SceneItemPrice existing = requirePrice(id);
        SceneItemPrice update = new SceneItemPrice();
        update.setId(existing.getId());

        if (dto.getPriceType() != null) update.setPriceType(dto.getPriceType());
        if (dto.getOriginalPrice() != null) update.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getSalePrice() != null) update.setSalePrice(dto.getSalePrice());
        if (dto.getChannelPrice() != null) update.setChannelPrice(dto.getChannelPrice());
        if (dto.getPriceDescription() != null) update.setPriceDescription(dto.getPriceDescription());
        if (dto.getEffectiveDate() != null) update.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getExpireDate() != null) update.setExpireDate(dto.getExpireDate());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        // 更新后再次校验日期区间
        LocalDate newEff = dto.getEffectiveDate() != null ? dto.getEffectiveDate() : existing.getEffectiveDate();
        LocalDate newExp = dto.getExpireDate() != null ? dto.getExpireDate() : existing.getExpireDate();
        validateDateRange(newEff, newExp);

        sceneItemPriceMapper.updateById(update);
        log.info("更新场景项目定价成功: id={}, sceneCode={}", id, existing.getSceneCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SceneItemPrice existing = requirePrice(id);
        sceneItemPriceMapper.deleteById(existing.getId());
        log.info("删除场景项目定价成功: id={}, sceneCode={}", id, existing.getSceneCode());
    }

    // ====== 内部方法 ======

    /** 校验生效/失效日期：若同时提供，effectiveDate 必须 ≤ expireDate */
    private void validateDateRange(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && effectiveDate.isAfter(expireDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "生效日期不能晚于失效日期: effective=" + effectiveDate + ", expire=" + expireDate);
        }
    }

    private LambdaQueryWrapper<SceneItemPrice> buildQueryWrapper(SceneItemPriceQueryDTO query) {
        LambdaQueryWrapper<SceneItemPrice> wrapper = new LambdaQueryWrapper<SceneItemPrice>()
                .eq(query.getSceneCode() != null && !query.getSceneCode().isEmpty(),
                        SceneItemPrice::getSceneCode, query.getSceneCode())
                .eq(query.getSceneItemCode() != null && !query.getSceneItemCode().isEmpty(),
                        SceneItemPrice::getSceneItemCode, query.getSceneItemCode())
                .eq(query.getPriceType() != null,
                        SceneItemPrice::getPriceType, query.getPriceType())
                .eq(query.getStatus() != null,
                        SceneItemPrice::getStatus, query.getStatus());
        if (query.getActiveOn() != null) {
            LocalDate active = query.getActiveOn();
            wrapper.and(w -> w.isNull(SceneItemPrice::getEffectiveDate)
                            .or().le(SceneItemPrice::getEffectiveDate, active))
                    .and(w -> w.isNull(SceneItemPrice::getExpireDate)
                            .or().ge(SceneItemPrice::getExpireDate, active));
        }
        wrapper.orderByDesc(SceneItemPrice::getCreatedAt);
        return wrapper;
    }

    private SceneItemPrice requirePrice(Long id) {
        SceneItemPrice price = sceneItemPriceMapper.selectById(id);
        if (price == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景项目定价不存在: " + id);
        }
        return price;
    }

    private SceneItemPriceVO toVO(SceneItemPrice entity) {
        SceneItemPriceVO vo = new SceneItemPriceVO();
        vo.setId(entity.getId());
        vo.setSceneCode(entity.getSceneCode());
        vo.setSceneItemCode(entity.getSceneItemCode());
        vo.setPriceType(entity.getPriceType());
        vo.setOriginalPrice(entity.getOriginalPrice());
        vo.setSalePrice(entity.getSalePrice());
        vo.setChannelPrice(entity.getChannelPrice());
        vo.setPriceDescription(entity.getPriceDescription());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
