package com.dayan.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dayan.client.dto.ClientAddressCreateDTO;
import com.dayan.client.dto.ClientAddressUpdateDTO;
import com.dayan.client.entity.ClientAddress;
import com.dayan.client.mapper.ClientAddressMapper;
import com.dayan.client.service.ClientAddressService;
import com.dayan.client.vo.ClientAddressVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户收货地址服务实现。
 *
 * <p>≤20 条地址限制；setDefault 先把同 client_code 的 is_default 全置 0，再置当前为 1。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAddressServiceImpl implements ClientAddressService {

    /** 单客户地址上限 */
    private static final int MAX_ADDRESS_PER_CLIENT = 20;

    private final ClientAddressMapper addressMapper;

    @Override
    public List<ClientAddressVO> listByClient(String clientCode) {
        List<ClientAddress> list = addressMapper.selectList(new LambdaQueryWrapper<ClientAddress>()
                .eq(ClientAddress::getClientCode, clientCode)
                .orderByDesc(ClientAddress::getIsDefault)
                .orderByDesc(ClientAddress::getCreatedAt));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ClientAddressCreateDTO dto) {
        // ≤20 条地址限制校验
        Long count = addressMapper.selectCount(new LambdaQueryWrapper<ClientAddress>()
                .eq(ClientAddress::getClientCode, dto.getClientCode()));
        if (count >= MAX_ADDRESS_PER_CLIENT) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "客户地址数量已达上限（" + MAX_ADDRESS_PER_CLIENT + " 条）");
        }

        ClientAddress entity = new ClientAddress();
        entity.setClientCode(dto.getClientCode());
        entity.setReceiverName(dto.getReceiverName());
        entity.setReceiverPhone(dto.getReceiverPhone());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setDetailAddress(dto.getDetailAddress());
        entity.setFullAddress(dto.getFullAddress());
        entity.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());
        entity.setTag(dto.getTag());
        // 若新建为默认地址，先把同 client_code 的其他地址 is_default 置 0
        if (entity.getIsDefault() == 1) {
            clearDefault(dto.getClientCode());
        }
        addressMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ClientAddressUpdateDTO dto) {
        ClientAddress existing = selectById(id);
        ClientAddress update = new ClientAddress();
        update.setId(existing.getId());
        if (dto.getReceiverName() != null) update.setReceiverName(dto.getReceiverName());
        if (dto.getReceiverPhone() != null) update.setReceiverPhone(dto.getReceiverPhone());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getDetailAddress() != null) update.setDetailAddress(dto.getDetailAddress());
        if (dto.getFullAddress() != null) update.setFullAddress(dto.getFullAddress());
        if (dto.getTag() != null) update.setTag(dto.getTag());
        // 若改为默认地址，先把同 client_code 的其他地址 is_default 置 0
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefault(existing.getClientCode());
        }
        if (dto.getIsDefault() != null) update.setIsDefault(dto.getIsDefault());
        addressMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        ClientAddress existing = selectById(id);
        // 先把同 client_code 的 is_default 全置 0
        clearDefault(existing.getClientCode());
        // 再置当前为 1
        ClientAddress update = new ClientAddress();
        update.setId(existing.getId());
        update.setIsDefault(1);
        addressMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        selectById(id);
        addressMapper.deleteById(id);
    }

    /** 把同 client_code 的所有地址 is_default 置 0 */
    private void clearDefault(String clientCode) {
        ClientAddress reset = new ClientAddress();
        reset.setIsDefault(0);
        addressMapper.update(reset, new LambdaUpdateWrapper<ClientAddress>()
                .eq(ClientAddress::getClientCode, clientCode)
                .eq(ClientAddress::getIsDefault, 1));
    }

    private ClientAddress selectById(Long id) {
        ClientAddress entity = addressMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在: " + id);
        }
        return entity;
    }

    private ClientAddressVO toVO(ClientAddress entity) {
        ClientAddressVO vo = new ClientAddressVO();
        vo.setId(entity.getId());
        vo.setClientCode(entity.getClientCode());
        vo.setReceiverName(entity.getReceiverName());
        vo.setReceiverPhone(entity.getReceiverPhone());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setDetailAddress(entity.getDetailAddress());
        vo.setFullAddress(entity.getFullAddress());
        vo.setIsDefault(entity.getIsDefault());
        vo.setTag(entity.getTag());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
