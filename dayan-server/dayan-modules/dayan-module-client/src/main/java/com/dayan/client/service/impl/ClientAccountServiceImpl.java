package com.dayan.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.client.dto.ClientAccountCreateDTO;
import com.dayan.client.dto.ClientAccountQueryDTO;
import com.dayan.client.dto.ClientAccountUpdateDTO;
import com.dayan.client.entity.ClientAccount;
import com.dayan.client.entity.ClientInfo;
import com.dayan.client.mapper.ClientAccountMapper;
import com.dayan.client.mapper.ClientInfoMapper;
import com.dayan.client.service.ClientAccountService;
import com.dayan.client.vo.ClientAccountVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.security.password.PasswordService;
import com.dayan.common.security.secret.DayanSecrets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户账号服务实现（按 channel_code 隔离，复用 PasswordService BCrypt）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAccountServiceImpl implements ClientAccountService {

    /** 重置密码默认值：由 DayanSecrets 单点配置（生产必须显式配置） */
    private final DayanSecrets dayanSecrets;

    private final ClientAccountMapper accountMapper;
    private final ClientInfoMapper clientInfoMapper;
    private final PasswordService passwordService;

    @Override
    public PageResult<ClientAccountVO> page(ClientAccountQueryDTO query) {
        LambdaQueryWrapper<ClientAccount> wrapper = new LambdaQueryWrapper<ClientAccount>()
                .eq(ClientAccount::getChannelCode, query.getChannelCode())
                .orderByDesc(ClientAccount::getCreatedAt);
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ClientAccount::getClientCode, query.getClientCode());
        }
        if (query.getUsername() != null && !query.getUsername().isEmpty()) {
            wrapper.like(ClientAccount::getUsername, query.getUsername());
        }
        if (query.getPhone() != null && !query.getPhone().isEmpty()) {
            wrapper.eq(ClientAccount::getPhone, query.getPhone());
        }
        if (query.getAccountStatus() != null) {
            wrapper.eq(ClientAccount::getAccountStatus, query.getAccountStatus());
        }
        Page<ClientAccount> page = accountMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ClientAccountVO> records = page.getRecords().stream().map(this::toVO).toList();
        fillRealName(records);
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ClientAccountCreateDTO dto) {
        // channel + username 唯一校验（若提供 username）
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            Long count = accountMapper.selectCount(new LambdaQueryWrapper<ClientAccount>()
                    .eq(ClientAccount::getChannelCode, dto.getChannelCode())
                    .eq(ClientAccount::getUsername, dto.getUsername()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "用户名已存在: " + dto.getUsername());
            }
        }
        // channel + phone 唯一校验（若提供 phone）
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            Long count = accountMapper.selectCount(new LambdaQueryWrapper<ClientAccount>()
                    .eq(ClientAccount::getChannelCode, dto.getChannelCode())
                    .eq(ClientAccount::getPhone, dto.getPhone()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "手机号已被占用: " + dto.getPhone());
            }
        }

        ClientAccount entity = new ClientAccount();
        entity.setClientCode(dto.getClientCode());
        entity.setChannelCode(dto.getChannelCode());
        entity.setUsername(dto.getUsername());
        entity.setPhone(dto.getPhone());
        // BCrypt 哈希密码（未提供则使用默认密码）
        String rawPwd = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? dayanSecrets.getDefaultResetPassword() : dto.getPassword();
        entity.setPassword(passwordService.encode(rawPwd));
        entity.setSalt("bcrypt");
        entity.setOpenId(dto.getOpenId());
        entity.setUnionId(dto.getUnionId());
        entity.setAlipayId(dto.getAlipayId());
        entity.setExtAccountNo(dto.getExtAccountNo());
        entity.setLoginCount(0);
        entity.setAccountStatus(dto.getAccountStatus() == null ? 1 : dto.getAccountStatus());
        accountMapper.insert(entity);
        return entity.getClientCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String clientCode, ClientAccountUpdateDTO dto) {
        ClientAccount existing = selectByCode(clientCode);
        ClientAccount update = new ClientAccount();
        update.setId(existing.getId());
        if (dto.getUsername() != null) {
            // 唯一性校验（排除自身）
            Long count = accountMapper.selectCount(new LambdaQueryWrapper<ClientAccount>()
                    .eq(ClientAccount::getChannelCode, existing.getChannelCode())
                    .eq(ClientAccount::getUsername, dto.getUsername())
                    .ne(ClientAccount::getId, existing.getId()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "用户名已存在: " + dto.getUsername());
            }
            update.setUsername(dto.getUsername());
        }
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getOpenId() != null) update.setOpenId(dto.getOpenId());
        if (dto.getUnionId() != null) update.setUnionId(dto.getUnionId());
        if (dto.getAlipayId() != null) update.setAlipayId(dto.getAlipayId());
        if (dto.getExtAccountNo() != null) update.setExtAccountNo(dto.getExtAccountNo());
        if (dto.getAccountStatus() != null) update.setAccountStatus(dto.getAccountStatus());
        // 不允许通过 update 改密码
        accountMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String clientCode) {
        ClientAccount existing = selectByCode(clientCode);
        ClientAccount update = new ClientAccount();
        update.setId(existing.getId());
        update.setPassword(passwordService.encode(dayanSecrets.getDefaultResetPassword()));
        accountMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String clientCode) {
        ClientAccount existing = selectByCode(clientCode);
        accountMapper.deleteById(existing.getId());
    }

    private ClientAccount selectByCode(String clientCode) {
        ClientAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ClientAccount>()
                .eq(ClientAccount::getClientCode, clientCode).last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户账号不存在: " + clientCode);
        }
        return account;
    }

    private ClientAccountVO toVO(ClientAccount entity) {
        ClientAccountVO vo = new ClientAccountVO();
        vo.setId(entity.getId());
        vo.setClientCode(entity.getClientCode());
        vo.setChannelCode(entity.getChannelCode());
        vo.setUsername(entity.getUsername());
        vo.setPhone(entity.getPhone());
        vo.setOpenId(entity.getOpenId());
        vo.setUnionId(entity.getUnionId());
        vo.setAlipayId(entity.getAlipayId());
        vo.setExtAccountNo(entity.getExtAccountNo());
        vo.setLastLoginTime(entity.getLastLoginTime());
        vo.setLastLoginIp(entity.getLastLoginIp());
        vo.setLoginCount(entity.getLoginCount());
        vo.setAccountStatus(entity.getAccountStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    /**
     * 批量回填真实姓名：按结果里出现的 clientCode 一次性查 client_info 取 fullName，
     * 组装 Map&lt;clientCode, fullName&gt; 回填到每个 VO，避免 N+1（channel_code 由租户拦截器自动追加）。
     */
    private void fillRealName(List<ClientAccountVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<String> codes = records.stream()
                .map(ClientAccountVO::getClientCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return;
        }
        List<ClientInfo> infos = clientInfoMapper.selectList(new LambdaQueryWrapper<ClientInfo>()
                .in(ClientInfo::getClientCode, codes)
                .select(ClientInfo::getClientCode, ClientInfo::getFullName));
        Map<String, String> nameMap = infos.stream()
                .collect(Collectors.toMap(ClientInfo::getClientCode, ClientInfo::getFullName, (a, b) -> a));
        records.forEach(vo -> vo.setRealName(nameMap.get(vo.getClientCode())));
    }
}
