package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkRoomPriceCreateDTO;
import com.dayan.park.dto.ParkRoomPriceQueryDTO;
import com.dayan.park.dto.ParkRoomPriceUpdateDTO;
import com.dayan.park.entity.ParkRoomPrice;
import com.dayan.park.mapper.ParkRoomPriceMapper;
import com.dayan.park.service.ParkRoomPriceService;
import com.dayan.park.vo.ParkRoomPriceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 房型价格（park_room_price）服务实现。
 *
 * <p>校验规则：
 * <ul>
 *   <li>effectiveDate &lt; expireDate（生效日期必须早于失效日期）</li>
 *   <li>isCurrent=1 在同 roomTypeCode 下唯一（设当前价时自动置 0 其余）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkRoomPriceServiceImpl implements ParkRoomPriceService {

    private static final int IS_CURRENT_YES = 1;

    private final ParkRoomPriceMapper roomPriceMapper;

    @Override
    public PageResult<ParkRoomPriceVO> page(ParkRoomPriceQueryDTO query) {
        LambdaQueryWrapper<ParkRoomPrice> wrapper = buildWrapper(query);
        Page<ParkRoomPrice> page = roomPriceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkRoomPriceVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkRoomPriceVO> listByRoomType(String parkCode, String roomTypeCode) {
        return roomPriceMapper.selectList(new LambdaQueryWrapper<ParkRoomPrice>()
                .eq(ParkRoomPrice::getParkCode, parkCode)
                .eq(ParkRoomPrice::getRoomTypeCode, roomTypeCode)
                .orderByAsc(ParkRoomPrice::getSortOrder)
                .orderByAsc(ParkRoomPrice::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkRoomPriceVO getDetail(Long id) {
        return toVO(requirePrice(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkRoomPriceCreateDTO dto) {
        validateDateRange(dto.getEffectiveDate(), dto.getExpireDate());
        Integer isCurrent = (dto.getIsCurrent() != null && dto.getIsCurrent() == IS_CURRENT_YES)
                ? IS_CURRENT_YES : 0;
        if (isCurrent == IS_CURRENT_YES) {
            clearOtherCurrent(dto.getRoomTypeCode(), null);
        }

        ParkRoomPrice entity = new ParkRoomPrice();
        entity.setParkCode(dto.getParkCode());
        entity.setRoomTypeCode(dto.getRoomTypeCode());
        entity.setPriceType(dto.getPriceType());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setDiscountRate(dto.getDiscountRate());
        entity.setPriceDescription(dto.getPriceDescription());
        entity.setIncludesItems(dto.getIncludesItems());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setExpireDate(dto.getExpireDate());
        entity.setIsCurrent(isCurrent);
        entity.setIsPromotion(dto.getIsPromotion() == null ? 0 : dto.getIsPromotion());
        entity.setPromotionDescription(dto.getPromotionDescription());
        entity.setPriceChangeReason(dto.getPriceChangeReason());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        roomPriceMapper.insert(entity);
        log.info("创建房型价格成功: parkCode={}, roomTypeCode={}, id={}",
                dto.getParkCode(), dto.getRoomTypeCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkRoomPriceUpdateDTO dto) {
        ParkRoomPrice existing = requirePrice(id);
        // 日期校验：合并已有值与新值
        LocalDate effective = dto.getEffectiveDate() != null ? dto.getEffectiveDate() : existing.getEffectiveDate();
        LocalDate expire = dto.getExpireDate() != null ? dto.getExpireDate() : existing.getExpireDate();
        validateDateRange(effective, expire);

        ParkRoomPrice update = new ParkRoomPrice();
        update.setId(existing.getId());
        if (dto.getPriceType() != null) update.setPriceType(dto.getPriceType());
        if (dto.getOriginalPrice() != null) update.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getSalePrice() != null) update.setSalePrice(dto.getSalePrice());
        if (dto.getDiscountRate() != null) update.setDiscountRate(dto.getDiscountRate());
        if (dto.getPriceDescription() != null) update.setPriceDescription(dto.getPriceDescription());
        if (dto.getIncludesItems() != null) update.setIncludesItems(dto.getIncludesItems());
        if (dto.getEffectiveDate() != null) update.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getExpireDate() != null) update.setExpireDate(dto.getExpireDate());
        if (dto.getIsPromotion() != null) update.setIsPromotion(dto.getIsPromotion());
        if (dto.getPromotionDescription() != null) update.setPromotionDescription(dto.getPromotionDescription());
        if (dto.getPriceChangeReason() != null) update.setPriceChangeReason(dto.getPriceChangeReason());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getIsCurrent() != null) {
            int isCurrent = (dto.getIsCurrent() == IS_CURRENT_YES) ? IS_CURRENT_YES : 0;
            if (isCurrent == IS_CURRENT_YES) {
                clearOtherCurrent(existing.getRoomTypeCode(), id);
            }
            update.setIsCurrent(isCurrent);
        }
        roomPriceMapper.updateById(update);
        log.info("更新房型价格成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkRoomPrice existing = requirePrice(id);
        roomPriceMapper.deleteById(existing.getId());
        log.info("删除房型价格成功: id={}", id);
    }

    // ====== 内部方法 ======

    private void validateDateRange(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && !effectiveDate.isBefore(expireDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "生效日期(effectiveDate)必须早于失效日期(expireDate)");
        }
    }

    /** 清除同 roomTypeCode 下其他价格的当前价标记（排除 excludeId） */
    private void clearOtherCurrent(String roomTypeCode, Long excludeId) {
        LambdaUpdateWrapper<ParkRoomPrice> wrapper = new LambdaUpdateWrapper<ParkRoomPrice>()
                .eq(ParkRoomPrice::getRoomTypeCode, roomTypeCode)
                .eq(ParkRoomPrice::getIsCurrent, IS_CURRENT_YES)
                .set(ParkRoomPrice::getIsCurrent, 0);
        if (excludeId != null) {
            wrapper.ne(ParkRoomPrice::getId, excludeId);
        }
        roomPriceMapper.update(null, wrapper);
    }

    private LambdaQueryWrapper<ParkRoomPrice> buildWrapper(ParkRoomPriceQueryDTO query) {
        LambdaQueryWrapper<ParkRoomPrice> wrapper = new LambdaQueryWrapper<ParkRoomPrice>()
                .orderByAsc(ParkRoomPrice::getSortOrder)
                .orderByDesc(ParkRoomPrice::getIsCurrent)
                .orderByAsc(ParkRoomPrice::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkRoomPrice::getParkCode, query.getParkCode());
        }
        if (query.getRoomTypeCode() != null && !query.getRoomTypeCode().isEmpty()) {
            wrapper.eq(ParkRoomPrice::getRoomTypeCode, query.getRoomTypeCode());
        }
        if (query.getPriceType() != null) {
            wrapper.eq(ParkRoomPrice::getPriceType, query.getPriceType());
        }
        if (query.getIsCurrent() != null) {
            wrapper.eq(ParkRoomPrice::getIsCurrent, query.getIsCurrent());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkRoomPrice::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkRoomPrice requirePrice(Long id) {
        ParkRoomPrice entity = roomPriceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "房型价格不存在: id=" + id);
        }
        return entity;
    }

    private ParkRoomPriceVO toVO(ParkRoomPrice entity) {
        ParkRoomPriceVO vo = new ParkRoomPriceVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setRoomTypeCode(entity.getRoomTypeCode());
        vo.setPriceType(entity.getPriceType());
        vo.setOriginalPrice(entity.getOriginalPrice());
        vo.setSalePrice(entity.getSalePrice());
        vo.setDiscountRate(entity.getDiscountRate());
        vo.setPriceDescription(entity.getPriceDescription());
        vo.setIncludesItems(entity.getIncludesItems());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setIsCurrent(entity.getIsCurrent());
        vo.setIsPromotion(entity.getIsPromotion());
        vo.setPromotionDescription(entity.getPromotionDescription());
        vo.setPriceChangeReason(entity.getPriceChangeReason());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
