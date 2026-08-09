package com.dayan.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.client.dto.ClientInfoCreateDTO;
import com.dayan.client.dto.ClientInfoQueryDTO;
import com.dayan.client.dto.ClientInfoUpdateDTO;
import com.dayan.client.entity.ClientInfo;
import com.dayan.client.mapper.ClientInfoMapper;
import com.dayan.client.service.ClientInfoService;
import com.dayan.client.vo.ClientInfoVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户信息服务实现（按渠道隔离）。
 *
 * <p>client_code 用 CL 前缀，渠道内唯一（uk_channel_client_code）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientInfoServiceImpl implements ClientInfoService {

    private final ClientInfoMapper clientInfoMapper;

    @Override
    public PageResult<ClientInfoVO> page(ClientInfoQueryDTO query) {
        LambdaQueryWrapper<ClientInfo> wrapper = new LambdaQueryWrapper<ClientInfo>()
                .orderByDesc(ClientInfo::getCreatedAt);
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(ClientInfo::getChannelCode, query.getChannelCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ClientInfo::getClientCode, query.getClientCode());
        }
        if (query.getFullName() != null && !query.getFullName().isEmpty()) {
            wrapper.like(ClientInfo::getFullName, query.getFullName());
        }
        if (query.getPhone() != null && !query.getPhone().isEmpty()) {
            wrapper.eq(ClientInfo::getPhone, query.getPhone());
        }
        if (query.getGender() != null) {
            wrapper.eq(ClientInfo::getGender, query.getGender());
        }
        if (query.getClientLevel() != null) {
            wrapper.eq(ClientInfo::getClientLevel, query.getClientLevel());
        }
        if (query.getIsVip() != null) {
            wrapper.eq(ClientInfo::getIsVip, query.getIsVip());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ClientInfo::getStatus, query.getStatus());
        }
        if (query.getSourceType() != null) {
            wrapper.eq(ClientInfo::getSourceType, query.getSourceType());
        }
        Page<ClientInfo> page = clientInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ClientInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public ClientInfoVO getDetail(String clientCode) {
        return toVO(selectByCode(clientCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ClientInfoCreateDTO dto) {
        ClientInfo entity = new ClientInfo();
        entity.setClientCode(generateClientCode());
        entity.setChannelCode(dto.getChannelCode());
        entity.setFullName(dto.getFullName());
        entity.setGender(dto.getGender());
        entity.setAvatar(dto.getAvatar());
        entity.setBirthday(dto.getBirthday());
        entity.setAge(dto.getAge());
        entity.setIdCard(dto.getIdCard());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setProvinceCode(dto.getProvinceCode());
        entity.setCityCode(dto.getCityCode());
        entity.setDistrictCode(dto.getDistrictCode());
        entity.setAddress(dto.getAddress());
        entity.setNationality(dto.getNationality());
        entity.setEthnic(dto.getEthnic());
        entity.setEducation(dto.getEducation());
        entity.setMaritalStatus(dto.getMaritalStatus());
        entity.setProfession(dto.getProfession());
        entity.setSourceType(dto.getSourceType());
        entity.setSourceAgentCode(dto.getSourceAgentCode());
        entity.setSourceChannelCode(dto.getSourceChannelCode());
        entity.setClientLevel(dto.getClientLevel() == null ? 0 : dto.getClientLevel());
        entity.setEquityCount(0);
        entity.setUsedEquityCount(0);
        entity.setServiceCount(0);
        entity.setTotalOrderAmount(java.math.BigDecimal.ZERO);
        entity.setRegisterTime(dto.getRegisterTime() == null ? LocalDateTime.now() : dto.getRegisterTime());
        entity.setIsVip(dto.getIsVip() == null ? 0 : dto.getIsVip());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        clientInfoMapper.insert(entity);
        return entity.getClientCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String clientCode, ClientInfoUpdateDTO dto) {
        ClientInfo existing = selectByCode(clientCode);
        ClientInfo update = new ClientInfo();
        update.setId(existing.getId());
        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getGender() != null) update.setGender(dto.getGender());
        if (dto.getAvatar() != null) update.setAvatar(dto.getAvatar());
        if (dto.getBirthday() != null) update.setBirthday(dto.getBirthday());
        if (dto.getAge() != null) update.setAge(dto.getAge());
        if (dto.getIdCard() != null) update.setIdCard(dto.getIdCard());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getEmail() != null) update.setEmail(dto.getEmail());
        if (dto.getProvinceCode() != null) update.setProvinceCode(dto.getProvinceCode());
        if (dto.getCityCode() != null) update.setCityCode(dto.getCityCode());
        if (dto.getDistrictCode() != null) update.setDistrictCode(dto.getDistrictCode());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getNationality() != null) update.setNationality(dto.getNationality());
        if (dto.getEthnic() != null) update.setEthnic(dto.getEthnic());
        if (dto.getEducation() != null) update.setEducation(dto.getEducation());
        if (dto.getMaritalStatus() != null) update.setMaritalStatus(dto.getMaritalStatus());
        if (dto.getProfession() != null) update.setProfession(dto.getProfession());
        if (dto.getSourceType() != null) update.setSourceType(dto.getSourceType());
        if (dto.getSourceAgentCode() != null) update.setSourceAgentCode(dto.getSourceAgentCode());
        if (dto.getSourceChannelCode() != null) update.setSourceChannelCode(dto.getSourceChannelCode());
        if (dto.getClientLevel() != null) update.setClientLevel(dto.getClientLevel());
        if (dto.getIsVip() != null) update.setIsVip(dto.getIsVip());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        clientInfoMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String clientCode) {
        selectByCode(clientCode);
        clientInfoMapper.delete(new LambdaQueryWrapper<ClientInfo>()
                .eq(ClientInfo::getClientCode, clientCode));
    }

    private ClientInfo selectByCode(String clientCode) {
        ClientInfo client = clientInfoMapper.selectOne(new LambdaQueryWrapper<ClientInfo>()
                .eq(ClientInfo::getClientCode, clientCode).last("LIMIT 1"));
        if (client == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户不存在: " + clientCode);
        }
        return client;
    }

    /** 简易编码生成：CL + 时间戳后 5 位 + 随机 3 位 */
    private String generateClientCode() {
        long ts = System.currentTimeMillis() % 100000L;
        int rand = (int) (Math.random() * 1000);
        return String.format("CL%05d%03d", ts, rand);
    }

    private ClientInfoVO toVO(ClientInfo entity) {
        ClientInfoVO vo = new ClientInfoVO();
        vo.setId(entity.getId());
        vo.setClientCode(entity.getClientCode());
        vo.setChannelCode(entity.getChannelCode());
        vo.setFullName(entity.getFullName());
        vo.setGender(entity.getGender());
        vo.setAvatar(entity.getAvatar());
        vo.setBirthday(entity.getBirthday());
        vo.setAge(entity.getAge());
        vo.setIdCard(entity.getIdCard());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setProvinceCode(entity.getProvinceCode());
        vo.setCityCode(entity.getCityCode());
        vo.setDistrictCode(entity.getDistrictCode());
        vo.setAddress(entity.getAddress());
        vo.setNationality(entity.getNationality());
        vo.setEthnic(entity.getEthnic());
        vo.setEducation(entity.getEducation());
        vo.setMaritalStatus(entity.getMaritalStatus());
        vo.setProfession(entity.getProfession());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceAgentCode(entity.getSourceAgentCode());
        vo.setSourceChannelCode(entity.getSourceChannelCode());
        vo.setClientLevel(entity.getClientLevel());
        vo.setEquityCount(entity.getEquityCount());
        vo.setUsedEquityCount(entity.getUsedEquityCount());
        vo.setServiceCount(entity.getServiceCount());
        vo.setTotalOrderAmount(entity.getTotalOrderAmount());
        vo.setLastServiceTime(entity.getLastServiceTime());
        vo.setRegisterTime(entity.getRegisterTime());
        vo.setLastLoginTime(entity.getLastLoginTime());
        vo.setIsVip(entity.getIsVip());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
