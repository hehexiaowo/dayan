package com.dayan.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.entity.ChannelInfo;
import com.dayan.channel.mapper.ChannelInfoMapper;
import com.dayan.client.dto.ClientProfileUpdateDTO;
import com.dayan.client.entity.ClientInfo;
import com.dayan.client.mapper.ClientInfoMapper;
import com.dayan.client.service.ClientProfileService;
import com.dayan.client.vo.ClientProfileVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.entity.SystemDictRegion;
import com.dayan.system.mapper.SystemDictRegionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Client 端个人资料服务实现。
 *
 * <p>当前登录人定位：{@code ContextHolder.getAccountCode()}（SaTokenContextFilter 写入的 loginId=clientCode）。
 * 敏感字段（phone/idCard）服务端脱敏后返回，仅用于展示。
 *
 * <p>逻辑镜像 {@code AgentProfileServiceImpl}：渠道名 / 区划名批量回填、白名单字段更新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientProfileServiceImpl implements ClientProfileService {

    private final ClientInfoMapper clientInfoMapper;
    private final ChannelInfoMapper channelInfoMapper;
    private final SystemDictRegionMapper regionMapper;

    @Override
    public ClientProfileVO getProfile() {
        ClientInfo info = requireCurrentClient();

        ClientProfileVO.ClientProfileVOBuilder b = ClientProfileVO.builder()
                .clientCode(info.getClientCode())
                .channelCode(info.getChannelCode())
                .fullName(info.getFullName())
                .gender(info.getGender())
                .avatar(info.getAvatar())
                .phone(maskPhone(info.getPhone()))
                .email(info.getEmail())
                .birthday(info.getBirthday())
                .age(info.getAge())
                .idCard(maskIdCard(info.getIdCard()))
                .provinceCode(info.getProvinceCode())
                .cityCode(info.getCityCode())
                .districtCode(info.getDistrictCode())
                .address(info.getAddress())
                .clientLevel(info.getClientLevel())
                .isVip(info.getIsVip())
                .registerTime(info.getRegisterTime())
                .lastLoginTime(info.getLastLoginTime())
                .equityCount(info.getEquityCount())
                .usedEquityCount(info.getUsedEquityCount())
                .serviceCount(info.getServiceCount())
                .totalOrderAmount(info.getTotalOrderAmount())
                .lastServiceTime(info.getLastServiceTime());
        fillRegionNames(b, info);
        fillChannelName(b, info.getChannelCode());
        return b.build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(ClientProfileUpdateDTO dto) {
        ClientInfo info = requireCurrentClient();
        // 白名单逐字段拷贝到独立更新对象（勿用 BeanUtils，避免类型不匹配静默丢数据）
        ClientInfo update = new ClientInfo();
        update.setId(info.getId());
        if (StringUtils.hasText(dto.getFullName())) {
            update.setFullName(dto.getFullName());
        }
        if (dto.getGender() != null) {
            update.setGender(dto.getGender());
        }
        if (dto.getEmail() != null) {
            update.setEmail(dto.getEmail());
        }
        if (dto.getAvatar() != null) {
            update.setAvatar(dto.getAvatar());
        }
        if (dto.getBirthday() != null) {
            update.setBirthday(dto.getBirthday());
        }
        if (dto.getProvinceCode() != null) {
            update.setProvinceCode(dto.getProvinceCode());
        }
        if (dto.getCityCode() != null) {
            update.setCityCode(dto.getCityCode());
        }
        if (dto.getDistrictCode() != null) {
            update.setDistrictCode(dto.getDistrictCode());
        }
        if (dto.getAddress() != null) {
            update.setAddress(dto.getAddress());
        }
        clientInfoMapper.updateById(update);
        log.info("[Profile] 更新资料: clientCode={}", info.getClientCode());
    }

    // ==================== 内部方法 ====================

    /** 当前登录客户；未登录抛 10101（前端拦截器自动跳登录），资料缺失抛 404 */
    private ClientInfo requireCurrentClient() {
        String clientCode = ContextHolder.getAccountCode();
        if (!StringUtils.hasText(clientCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        ClientInfo info = clientInfoMapper.selectOne(new LambdaQueryWrapper<ClientInfo>()
                .eq(ClientInfo::getClientCode, clientCode).last("LIMIT 1"));
        if (info == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户资料不存在");
        }
        return info;
    }

    /** 渠道简称回填（缺失时降级 full_name） */
    private void fillChannelName(ClientProfileVO.ClientProfileVOBuilder b, String channelCode) {
        if (!StringUtils.hasText(channelCode)) {
            return;
        }
        ChannelInfo channel = channelInfoMapper.selectOne(new LambdaQueryWrapper<ChannelInfo>()
                .eq(ChannelInfo::getChannelCode, channelCode)
                .select(ChannelInfo::getChannelCode, ChannelInfo::getShortName, ChannelInfo::getFullName)
                .last("LIMIT 1"));
        if (channel != null) {
            b.channelName(StringUtils.hasText(channel.getShortName())
                    ? channel.getShortName() : channel.getFullName());
        }
    }

    /** 区划 code → name 批量回填（一次 in 查询；Stream.of 容忍 null，勿用 List.of） */
    private void fillRegionNames(ClientProfileVO.ClientProfileVOBuilder b, ClientInfo info) {
        List<String> codes = Stream.of(info.getProvinceCode(), info.getCityCode(), info.getDistrictCode())
                .filter(StringUtils::hasText).collect(Collectors.toList());
        if (codes.isEmpty()) {
            return;
        }
        List<SystemDictRegion> regions = regionMapper.selectList(new LambdaQueryWrapper<SystemDictRegion>()
                .in(SystemDictRegion::getRegionCode, codes)
                .select(SystemDictRegion::getRegionCode, SystemDictRegion::getRegionName));
        Map<String, String> nameMap = regions.stream()
                .collect(Collectors.toMap(SystemDictRegion::getRegionCode, SystemDictRegion::getRegionName, (a, c) -> a));
        b.provinceName(nameMap.get(info.getProvinceCode()))
                .cityName(nameMap.get(info.getCityCode()))
                .districtName(nameMap.get(info.getDistrictCode()));
    }

    /** 11 位手机号中间四位打码，其余原样返回 */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.length() == 11 ? phone.substring(0, 3) + "****" + phone.substring(7) : phone;
    }

    /** 身份证脱敏：保留前 6 后 4，中间打码；长度不足则原样 */
    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 10) {
            return idCard;
        }
        int len = idCard.length();
        StringBuilder sb = new StringBuilder(idCard.substring(0, 6));
        for (int i = 0; i < len - 10; i++) {
            sb.append('*');
        }
        sb.append(idCard.substring(len - 4));
        return sb.toString();
    }
}
