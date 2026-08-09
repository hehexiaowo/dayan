package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.goods.dto.GoodsEquitySaveDTO;
import com.dayan.goods.entity.GoodsEquity;
import com.dayan.goods.entity.GoodsServiceItemRel;
import com.dayan.goods.entity.ServiceItem;
import com.dayan.goods.mapper.GoodsEquityMapper;
import com.dayan.goods.mapper.GoodsServiceItemRelMapper;
import com.dayan.goods.mapper.ServiceItemMapper;
import com.dayan.goods.service.GoodsEquityService;
import com.dayan.goods.vo.GoodsEquityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsEquityServiceImpl implements GoodsEquityService {

    private final GoodsEquityMapper goodsEquityMapper;
    private final GoodsServiceItemRelMapper relMapper;
    private final ServiceItemMapper serviceItemMapper;

    @Override
    public GoodsEquityVO getByGoodsCode(String goodsCode) {
        LambdaQueryWrapper<GoodsEquity> wrapper = new LambdaQueryWrapper<GoodsEquity>()
                .eq(GoodsEquity::getGoodsCode, goodsCode);
        GoodsEquity entity = goodsEquityMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        return toVO(entity, listRelsByGoodsCode(goodsCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(GoodsEquitySaveDTO dto) {
        // 1. UPSERT goods_equity（1:1 by goodsCode）
        LambdaQueryWrapper<GoodsEquity> wrapper = new LambdaQueryWrapper<GoodsEquity>()
                .eq(GoodsEquity::getGoodsCode, dto.getGoodsCode());
        GoodsEquity existing = goodsEquityMapper.selectOne(wrapper);

        if (existing == null) {
            existing = new GoodsEquity();
            existing.setGoodsCode(dto.getGoodsCode());
            existing.setPersonCount(dto.getPersonCount() != null ? dto.getPersonCount() : 1);
            existing.setValidDays(dto.getValidDays() != null ? dto.getValidDays() : 365);
            existing.setShelfLifeDays(dto.getShelfLifeDays() != null ? dto.getShelfLifeDays() : 730);
            existing.setMaxTransferable(dto.getMaxTransferable() != null ? dto.getMaxTransferable() : 1);
            existing.setDescription(dto.getDescription());
            existing.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
            existing.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
            goodsEquityMapper.insert(existing);
        } else {
            if (dto.getPersonCount() != null) existing.setPersonCount(dto.getPersonCount());
            if (dto.getValidDays() != null) existing.setValidDays(dto.getValidDays());
            if (dto.getShelfLifeDays() != null) existing.setShelfLifeDays(dto.getShelfLifeDays());
            if (dto.getMaxTransferable() != null) existing.setMaxTransferable(dto.getMaxTransferable());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
            if (dto.getSortOrder() != null) existing.setSortOrder(dto.getSortOrder());
            if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
            goodsEquityMapper.updateById(existing);
        }

        // 2. 先删后插 goods_service_item_rel（N:M，物理删除避免逻辑删除+唯一键冲突）
        relMapper.physicalDeleteByGoodsCode(dto.getGoodsCode());

        if (dto.getServiceItems() != null && !dto.getServiceItems().isEmpty()) {
            // 批量校验 itemCode 存在性 + 批量查 itemName
            Set<String> itemCodes = dto.getServiceItems().stream()
                    .map(GoodsEquitySaveDTO.ServiceItemRelDTO::getItemCode)
                    .collect(Collectors.toSet());
            LambdaQueryWrapper<ServiceItem> itemWrapper = new LambdaQueryWrapper<ServiceItem>()
                    .in(ServiceItem::getItemCode, itemCodes);
            List<ServiceItem> items = serviceItemMapper.selectList(itemWrapper);
            Set<String> foundCodes = items.stream()
                    .map(ServiceItem::getItemCode)
                    .collect(Collectors.toSet());
            for (String code : itemCodes) {
                if (!foundCodes.contains(code)) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "服务项目不存在：" + code);
                }
            }

            for (GoodsEquitySaveDTO.ServiceItemRelDTO relDto : dto.getServiceItems()) {
                GoodsServiceItemRel rel = new GoodsServiceItemRel();
                rel.setGoodsCode(dto.getGoodsCode());
                rel.setItemCode(relDto.getItemCode());
                rel.setQuantity(relDto.getQuantity());
                rel.setSortOrder(relDto.getSortOrder() != null ? relDto.getSortOrder() : 0);
                relMapper.insert(rel);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String goodsCode) {
        // 物理删除 rel + equity（避免逻辑删除+唯一键冲突）
        relMapper.physicalDeleteByGoodsCode(goodsCode);
        goodsEquityMapper.physicalDeleteByGoodsCode(goodsCode);
    }

    @Override
    public GoodsEquity requireByGoodsCode(String goodsCode) {
        LambdaQueryWrapper<GoodsEquity> wrapper = new LambdaQueryWrapper<GoodsEquity>()
                .eq(GoodsEquity::getGoodsCode, goodsCode);
        GoodsEquity entity = goodsEquityMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益商品配置不存在：" + goodsCode);
        }
        return entity;
    }

    @Override
    public List<GoodsEquityVO.ServiceItemRelVO> listRelsByGoodsCode(String goodsCode) {
        // 查 rel
        LambdaQueryWrapper<GoodsServiceItemRel> relWrapper = new LambdaQueryWrapper<GoodsServiceItemRel>()
                .eq(GoodsServiceItemRel::getGoodsCode, goodsCode)
                .orderByAsc(GoodsServiceItemRel::getSortOrder)
                .orderByAsc(GoodsServiceItemRel::getCreatedAt);
        List<GoodsServiceItemRel> rels = relMapper.selectList(relWrapper);
        if (rels.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查 service_item 防止 N+1
        Set<String> itemCodes = rels.stream()
                .map(GoodsServiceItemRel::getItemCode)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<ServiceItem> itemWrapper = new LambdaQueryWrapper<ServiceItem>()
                .in(ServiceItem::getItemCode, itemCodes);
        List<ServiceItem> items = serviceItemMapper.selectList(itemWrapper);
        Map<String, ServiceItem> itemMap = items.stream()
                .collect(Collectors.toMap(ServiceItem::getItemCode, i -> i));

        return rels.stream().map(rel -> {
            GoodsEquityVO.ServiceItemRelVO vo = new GoodsEquityVO.ServiceItemRelVO();
            vo.setId(rel.getId());
            vo.setGoodsCode(rel.getGoodsCode());
            vo.setItemCode(rel.getItemCode());
            ServiceItem item = itemMap.get(rel.getItemCode());
            if (item != null) {
                vo.setItemName(item.getItemName());
                vo.setItemCategory(item.getItemCategory());
                vo.setItemSubtype(item.getItemSubtype());
            }
            vo.setQuantity(rel.getQuantity());
            vo.setSortOrder(rel.getSortOrder());
            vo.setCreatedAt(rel.getCreatedAt());
            return vo;
        }).toList();
    }

    private GoodsEquityVO toVO(GoodsEquity entity, List<GoodsEquityVO.ServiceItemRelVO> serviceItems) {
        GoodsEquityVO vo = new GoodsEquityVO();
        vo.setId(entity.getId());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setPersonCount(entity.getPersonCount());
        vo.setValidDays(entity.getValidDays());
        vo.setShelfLifeDays(entity.getShelfLifeDays());
        vo.setMaxTransferable(entity.getMaxTransferable());
        vo.setDescription(entity.getDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setServiceItems(serviceItems);
        return vo;
    }
}
