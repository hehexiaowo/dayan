package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSojournCreateDTO;
import com.dayan.goods.dto.GoodsSojournQueryDTO;
import com.dayan.goods.dto.GoodsSojournUpdateDTO;
import com.dayan.goods.entity.GoodsSojourn;
import com.dayan.goods.mapper.GoodsSojournMapper;
import com.dayan.goods.service.GoodsSojournService;
import com.dayan.goods.vo.GoodsSojournVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 旅游短居 SKU（goods_sojourn）服务实现。
 *
 * <p>主键 id（AUTO_INCREMENT），业务键 skuCode（GJ + 5 位序列）。
 * 关联 {@code parkCode}/{@code roomTypeCode} 采用弱校验。时长范围 minDays/maxDays 校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsSojournServiceImpl implements GoodsSojournService {

    /** SKU 编码前缀（GJ = Goods SoJourn） */
    private static final String SKU_PREFIX = "GJ";
    private static final String SEQ_KEY = "code:seq:" + SKU_PREFIX + ":0";
    private static final int DEFAULT_STATUS = 1;

    private final GoodsSojournMapper sojournMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<GoodsSojournVO> page(GoodsSojournQueryDTO query) {
        LambdaQueryWrapper<GoodsSojourn> wrapper = buildWrapper(query);
        Page<GoodsSojourn> page = sojournMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<GoodsSojournVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<GoodsSojournVO> listByGoods(String goodsCode) {
        return sojournMapper.selectList(new LambdaQueryWrapper<GoodsSojourn>()
                .eq(GoodsSojourn::getGoodsCode, goodsCode)
                .orderByAsc(GoodsSojourn::getSortOrder)
                .orderByAsc(GoodsSojourn::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public GoodsSojournVO getDetail(Long id) {
        return toVO(requireSku(id));
    }

    @Override
    public GoodsSojournVO getByCode(String skuCode) {
        GoodsSojourn entity = sojournMapper.selectOne(new LambdaQueryWrapper<GoodsSojourn>()
                .eq(GoodsSojourn::getSkuCode, skuCode)
                .last("LIMIT 1"));
        return entity != null ? toVO(entity) : null;
    }

    @Override
    public GoodsSojournVO getEffectiveByCode(String skuCode) {
        if (skuCode == null || skuCode.isEmpty()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        GoodsSojourn entity = sojournMapper.selectList(new LambdaQueryWrapper<GoodsSojourn>()
                .eq(GoodsSojourn::getSkuCode, skuCode)
                .eq(GoodsSojourn::getStatus, 1)
                .and(w -> w.isNull(GoodsSojourn::getEffectiveDate)
                        .or().le(GoodsSojourn::getEffectiveDate, today))
                .and(w -> w.isNull(GoodsSojourn::getExpireDate)
                        .or().ge(GoodsSojourn::getExpireDate, today))
                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GoodsSojournCreateDTO dto) {
        validateDayRange(dto.getMinDays(), dto.getMaxDays());
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());

        GoodsSojourn entity = new GoodsSojourn();
        entity.setGoodsCode(dto.getGoodsCode());
        entity.setSkuCode(nextSkuCode());
        entity.setSkuName(dto.getSkuName());
        entity.setParkCode(dto.getParkCode());
        entity.setRoomTypeCode(dto.getRoomTypeCode());
        entity.setRoomTypeName(dto.getRoomTypeName());
        entity.setCareTypeCode(dto.getCareTypeCode());
        entity.setFoodTypeCode(dto.getFoodTypeCode());
        entity.setSkuPrice(dto.getSkuPrice());
        entity.setPriceUnit(dto.getPriceUnit());
        entity.setMinDays(dto.getMinDays());
        entity.setMaxDays(dto.getMaxDays());
        entity.setStock(dto.getStock());
        entity.setSalesCount(0);
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpireDate(dto.getExpireDate());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        sojournMapper.insert(entity);
        log.info("创建旅游短居 SKU 成功: goodsCode={}, skuCode={}, id={}",
                dto.getGoodsCode(), entity.getSkuCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoodsSojournUpdateDTO dto) {
        GoodsSojourn existing = requireSku(id);
        // 合并已有值后再校验区间
        Integer minDays = dto.getMinDays() != null ? dto.getMinDays() : existing.getMinDays();
        Integer maxDays = dto.getMaxDays() != null ? dto.getMaxDays() : existing.getMaxDays();
        validateDayRange(minDays, maxDays);
        LocalDate effective = dto.getEffectiveDate() != null ? dto.getEffectiveDate() : existing.getEffectiveDate();
        LocalDate expire = dto.getExpireDate() != null ? dto.getExpireDate() : existing.getExpireDate();
        validateDateRange(effective, expire);

        GoodsSojourn update = new GoodsSojourn();
        update.setId(existing.getId());

        if (dto.getSkuName() != null) update.setSkuName(dto.getSkuName());
        if (dto.getParkCode() != null) update.setParkCode(dto.getParkCode());
        if (dto.getRoomTypeCode() != null) update.setRoomTypeCode(dto.getRoomTypeCode());
        if (dto.getRoomTypeName() != null) update.setRoomTypeName(dto.getRoomTypeName());
        if (dto.getCareTypeCode() != null) update.setCareTypeCode(dto.getCareTypeCode());
        if (dto.getFoodTypeCode() != null) update.setFoodTypeCode(dto.getFoodTypeCode());
        if (dto.getSkuPrice() != null) update.setSkuPrice(dto.getSkuPrice());
        if (dto.getPriceUnit() != null) update.setPriceUnit(dto.getPriceUnit());
        if (dto.getMinDays() != null) update.setMinDays(dto.getMinDays());
        if (dto.getMaxDays() != null) update.setMaxDays(dto.getMaxDays());
        if (dto.getStock() != null) update.setStock(dto.getStock());
        if (dto.getEffectiveDate() != null) update.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getExpireDate() != null) update.setExpireDate(dto.getExpireDate());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        sojournMapper.updateById(update);
        log.info("更新旅游短居 SKU 成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GoodsSojourn existing = requireSku(id);
        sojournMapper.deleteById(existing.getId());
        log.info("删除旅游短居 SKU 成功: id={}", id);
    }

    // ====== 内部方法 ======

    /** 校验：最少天数 ≤ 最多天数 */
    private void validateDayRange(Integer minDays, Integer maxDays) {
        if (minDays != null && maxDays != null && minDays > maxDays) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "最少天数(minDays=" + minDays + ")不能大于最多天数(maxDays=" + maxDays + ")");
        }
    }

    /** 校验：生效日期 ≤ 失效日期 */
    private void validateDateRange(LocalDate effective, LocalDate expire) {
        if (effective != null && expire != null && effective.isAfter(expire)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "生效日期(effectiveDate=" + effective + ")不能晚于失效日期(expireDate=" + expire + ")");
        }
    }

    private LambdaQueryWrapper<GoodsSojourn> buildWrapper(GoodsSojournQueryDTO query) {
        LambdaQueryWrapper<GoodsSojourn> wrapper = new LambdaQueryWrapper<GoodsSojourn>()
                .orderByAsc(GoodsSojourn::getSortOrder)
                .orderByAsc(GoodsSojourn::getId);
        if (query.getGoodsCode() != null && !query.getGoodsCode().isEmpty()) {
            wrapper.eq(GoodsSojourn::getGoodsCode, query.getGoodsCode());
        }
        if (query.getSkuCode() != null && !query.getSkuCode().isEmpty()) {
            wrapper.eq(GoodsSojourn::getSkuCode, query.getSkuCode());
        }
        if (query.getSkuName() != null && !query.getSkuName().isEmpty()) {
            wrapper.like(GoodsSojourn::getSkuName, query.getSkuName());
        }
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(GoodsSojourn::getParkCode, query.getParkCode());
        }
        if (query.getRoomTypeCode() != null && !query.getRoomTypeCode().isEmpty()) {
            wrapper.eq(GoodsSojourn::getRoomTypeCode, query.getRoomTypeCode());
        }
        if (query.getStatus() != null) {
            wrapper.eq(GoodsSojourn::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private GoodsSojourn requireSku(Long id) {
        GoodsSojourn entity = sojournMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "旅游短居 SKU 不存在: id=" + id);
        }
        return entity;
    }

    private String nextSkuCode() {
        return SKU_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private GoodsSojournVO toVO(GoodsSojourn entity) {
        GoodsSojournVO vo = new GoodsSojournVO();
        vo.setId(entity.getId());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setSkuCode(entity.getSkuCode());
        vo.setSkuName(entity.getSkuName());
        vo.setParkCode(entity.getParkCode());
        vo.setRoomTypeCode(entity.getRoomTypeCode());
        vo.setRoomTypeName(entity.getRoomTypeName());
        vo.setCareTypeCode(entity.getCareTypeCode());
        vo.setFoodTypeCode(entity.getFoodTypeCode());
        vo.setSkuPrice(entity.getSkuPrice());
        vo.setPriceUnit(entity.getPriceUnit());
        vo.setMinDays(entity.getMinDays());
        vo.setMaxDays(entity.getMaxDays());
        vo.setStock(entity.getStock());
        vo.setSalesCount(entity.getSalesCount());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
