package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.goods.dto.ServiceItemCreateDTO;
import com.dayan.goods.dto.ServiceItemQueryDTO;
import com.dayan.goods.dto.ServiceItemUpdateDTO;
import com.dayan.goods.entity.ServiceItem;
import com.dayan.goods.mapper.ServiceItemMapper;
import com.dayan.goods.service.ServiceItemService;
import com.dayan.goods.vo.ServiceItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private static final String CODE_PREFIX = "SI";
    private static final String SEQ_KEY = "code:seq:SI:0";
    private static final int SEQ_WIDTH = 5;

    private final ServiceItemMapper serviceItemMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<ServiceItemVO> page(ServiceItemQueryDTO query) {
        Page<ServiceItem> page = new Page<>(query.getCurrent(), query.getSize());
        Page<ServiceItem> result = serviceItemMapper.selectPage(page, buildQueryWrapper(query));
        List<ServiceItemVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), result.getTotal(), records);
    }

    @Override
    public List<ServiceItemVO> list(ServiceItemQueryDTO query) {
        List<ServiceItem> list = serviceItemMapper.selectList(buildQueryWrapper(query));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public ServiceItemVO getDetail(String itemCode) {
        ServiceItem entity = requireItem(itemCode);
        return toVO(entity);
    }

    @Override
    public ServiceItem requireItem(String itemCode) {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getItemCode, itemCode);
        ServiceItem entity = serviceItemMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "服务项目不存在：" + itemCode);
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ServiceItemCreateDTO dto) {
        ServiceItem entity = new ServiceItem();
        entity.setItemCode(generateCode());
        entity.setItemName(dto.getItemName());
        entity.setItemCategory(dto.getItemCategory());
        entity.setItemSubtype(dto.getItemSubtype());
        entity.setItemValue(dto.getItemValue());
        entity.setCostBearing(dto.getCostBearing() != null ? dto.getCostBearing() : 0);
        // 网络范围：结构化落 service_network JSON（all/空 → NULL=业态全部机构）
        entity.setServiceNetwork(toNetworkJson(dto.getNetworkScope()));
        entity.setCoveredItems(dto.getCoveredItems());
        entity.setValidDays(dto.getValidDays() != null ? dto.getValidDays() : 365);
        entity.setMaxUseCount(dto.getMaxUseCount() != null ? dto.getMaxUseCount() : 1);
        entity.setDescription(dto.getDescription());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        serviceItemMapper.insert(entity);
        return entity.getItemCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String itemCode, ServiceItemUpdateDTO dto) {
        ServiceItem entity = requireItem(itemCode);
        if (dto.getItemName() != null) entity.setItemName(dto.getItemName());
        if (dto.getItemCategory() != null) entity.setItemCategory(dto.getItemCategory());
        if (dto.getItemSubtype() != null) entity.setItemSubtype(dto.getItemSubtype());
        if (dto.getItemValue() != null) entity.setItemValue(dto.getItemValue());
        if (dto.getCostBearing() != null) entity.setCostBearing(dto.getCostBearing());
        if (dto.getNetworkScope() != null) {
            // 传 all/空 parks = 恢复业态全部（存 NULL），custom = 自选范围
            entity.setServiceNetwork(toNetworkJson(dto.getNetworkScope()));
        }
        if (dto.getCoveredItems() != null) entity.setCoveredItems(dto.getCoveredItems());
        if (dto.getValidDays() != null) entity.setValidDays(dto.getValidDays());
        if (dto.getMaxUseCount() != null) entity.setMaxUseCount(dto.getMaxUseCount());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        serviceItemMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String itemCode) {
        ServiceItem entity = requireItem(itemCode);
        serviceItemMapper.deleteById(entity.getId());
    }

    private String generateCode() {
        long seq = sequenceProvider.next(SEQ_KEY);
        return CODE_PREFIX + String.format("%0" + SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<ServiceItem> buildQueryWrapper(ServiceItemQueryDTO query) {
        return new LambdaQueryWrapper<ServiceItem>()
                .like(query.getItemCode() != null && !query.getItemCode().isEmpty(),
                        ServiceItem::getItemCode, query.getItemCode())
                .like(query.getItemName() != null && !query.getItemName().isEmpty(),
                        ServiceItem::getItemName, query.getItemName())
                .eq(query.getItemCategory() != null, ServiceItem::getItemCategory, query.getItemCategory())
                .eq(query.getItemSubtype() != null, ServiceItem::getItemSubtype, query.getItemSubtype())
                .eq(query.getStatus() != null, ServiceItem::getStatus, query.getStatus())
                .orderByAsc(ServiceItem::getSortOrder)
                .orderByDesc(ServiceItem::getCreatedAt);
    }

    private ServiceItemVO toVO(ServiceItem entity) {
        ServiceItemVO vo = new ServiceItemVO();
        vo.setId(entity.getId());
        vo.setItemCode(entity.getItemCode());
        vo.setItemName(entity.getItemName());
        vo.setItemCategory(entity.getItemCategory());
        vo.setItemSubtype(entity.getItemSubtype());
        vo.setItemValue(entity.getItemValue());
        vo.setCostBearing(entity.getCostBearing());
        // 兼容旧数组格式（通配符/机构码）统一解析为结构化范围
        vo.setNetworkScope(com.dayan.goods.model.RightsJson.readNetwork(entity.getServiceNetwork()));
        vo.setCoveredItems(entity.getCoveredItems());
        vo.setValidDays(entity.getValidDays());
        vo.setMaxUseCount(entity.getMaxUseCount());
        vo.setDescription(entity.getDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /** 结构化范围 → service_network JSON：非 custom（全部）落 NULL */
    private String toNetworkJson(com.dayan.goods.model.NetworkScope scope) {
        if (scope == null || !scope.isCustom()) {
            return null;
        }
        return com.dayan.goods.model.RightsJson.write(scope);
    }
}
