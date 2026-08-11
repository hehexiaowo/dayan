package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.agent.dto.AgentProfileUpdateDTO;
import com.dayan.agent.entity.AgentAccount;
import com.dayan.agent.entity.AgentInfo;
import com.dayan.agent.mapper.AgentAccountMapper;
import com.dayan.agent.mapper.AgentInfoMapper;
import com.dayan.agent.service.AgentProfileService;
import com.dayan.agent.vo.AgentProfileVO;
import com.dayan.agent.vo.SmsSendVO;
import com.dayan.channel.entity.ChannelInfo;
import com.dayan.channel.mapper.ChannelInfoMapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.entity.SystemDictRegion;
import com.dayan.system.mapper.SystemDictRegionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Agent 端个人资料服务实现。
 *
 * <p>当前登录人定位：{@code ContextHolder.getAccountCode()}（SaTokenContextFilter 写入的 loginId=agentCode）。
 * agent_info 按 (channel_code, agent_code) 唯一键联查；agent_info 缺失时降级返回 account 级字段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProfileServiceImpl implements AgentProfileService {

    private final AgentAccountMapper accountMapper;
    private final AgentInfoMapper infoMapper;
    private final ChannelInfoMapper channelInfoMapper;
    private final SystemDictRegionMapper regionMapper;

    @Override
    public AgentProfileVO getProfile() {
        AgentAccount account = requireCurrentAccount();
        AgentInfo info = findInfo(account);

        AgentProfileVO.AgentProfileVOBuilder builder = AgentProfileVO.builder()
                .agentCode(account.getAgentCode())
                .username(account.getUsername())
                .phone(account.getPhone())
                .channelCode(account.getChannelCode())
                .lastLoginTime(account.getLastLoginTime());

        if (info != null) {
            builder.fullName(info.getFullName())
                    .gender(info.getGender())
                    .avatar(info.getAvatar())
                    .email(info.getEmail())
                    .companyName(info.getCompanyName())
                    .branchName(info.getBranchName())
                    .department(info.getDepartment())
                    .position(info.getPosition())
                    .employeeNo(info.getEmployeeNo())
                    .licenseNo(info.getLicenseNo())
                    .provinceCode(info.getProvinceCode())
                    .cityCode(info.getCityCode())
                    .districtCode(info.getDistrictCode())
                    .address(info.getAddress())
                    .serviceIntro(info.getServiceIntro())
                    .agentLevel(info.getAgentLevel())
                    .isCertified(info.getIsCertified());
            // agent_info.phone 与 account.phone 以 account 为准（换绑同事务同步两处）
            if (StringUtils.hasText(info.getPhone())) {
                builder.phone(info.getPhone());
            }
            fillRegionNames(builder, info);
        }

        ChannelInfo channel = channelInfoMapper.selectOne(new LambdaQueryWrapper<ChannelInfo>()
                .eq(ChannelInfo::getChannelCode, account.getChannelCode())
                .select(ChannelInfo::getChannelCode, ChannelInfo::getShortName)
                .last("LIMIT 1"));
        if (channel != null) {
            builder.channelName(channel.getShortName());
        }
        return builder.build();
    }

    @Override
    public void updateProfile(AgentProfileUpdateDTO dto) {
        throw new UnsupportedOperationException("任务 2 实现");
    }

    @Override
    public SmsSendVO sendPhoneChangeCode(String mobile) {
        throw new UnsupportedOperationException("任务 3 实现");
    }

    @Override
    public void changePhone(String mobile, String code) {
        throw new UnsupportedOperationException("任务 3 实现");
    }

    // ==================== 内部方法 ====================

    /** 当前登录代理人账号；未登录抛 10101（前端拦截器自动跳登录页） */
    private AgentAccount requireCurrentAccount() {
        String agentCode = ContextHolder.getAccountCode();
        if (!StringUtils.hasText(agentCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        AgentAccount account = accountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getAgentCode, agentCode)
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        return account;
    }

    private AgentInfo findInfo(AgentAccount account) {
        return infoMapper.selectOne(new LambdaQueryWrapper<AgentInfo>()
                .eq(AgentInfo::getChannelCode, account.getChannelCode())
                .eq(AgentInfo::getAgentCode, account.getAgentCode())
                .last("LIMIT 1"));
    }

    /** 区划 code → name 批量回填（一次 in 查询；Stream.of 容忍 null，勿用 List.of） */
    private void fillRegionNames(AgentProfileVO.AgentProfileVOBuilder builder, AgentInfo info) {
        List<String> codes = Stream.of(info.getProvinceCode(), info.getCityCode(), info.getDistrictCode())
                .filter(StringUtils::hasText).collect(Collectors.toList());
        if (codes.isEmpty()) {
            return;
        }
        List<SystemDictRegion> regions = regionMapper.selectList(new LambdaQueryWrapper<SystemDictRegion>()
                .in(SystemDictRegion::getRegionCode, codes)
                .select(SystemDictRegion::getRegionCode, SystemDictRegion::getRegionName));
        Map<String, String> nameMap = regions.stream()
                .collect(Collectors.toMap(SystemDictRegion::getRegionCode, SystemDictRegion::getRegionName, (a, b) -> a));
        builder.provinceName(nameMap.get(info.getProvinceCode()))
                .cityName(nameMap.get(info.getCityCode()))
                .districtName(nameMap.get(info.getDistrictCode()));
    }
}
