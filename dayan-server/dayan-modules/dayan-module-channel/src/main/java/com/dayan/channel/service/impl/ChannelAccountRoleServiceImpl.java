package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.entity.ChannelAccount;
import com.dayan.channel.entity.ChannelAccountRoleRel;
import com.dayan.channel.mapper.ChannelAccountMapper;
import com.dayan.channel.mapper.ChannelAccountRoleRelMapper;
import com.dayan.channel.service.ChannelAccountRoleService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 渠道账号-角色关联服务实现。
 *
 * <p>分配采用"先删后增"全量覆盖语义；关联记录的 channelCode 从账号本身取值，
 * 确保跨渠道账号关联记录不会因租户隔离而错位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelAccountRoleServiceImpl implements ChannelAccountRoleService {

    private final ChannelAccountMapper accountMapper;
    private final ChannelAccountRoleRelMapper accountRoleRelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(String accountCode, List<String> roleCodes) {
        // 校验账号存在，并取 channelCode
        ChannelAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "渠道账号不存在: " + accountCode);
        }

        // 先删后增（全量覆盖）：物理删除避免逻辑删除残留占 uk_account_role 唯一键
        accountRoleRelMapper.physicallyDeleteByAccountCode(accountCode);

        if (roleCodes == null || roleCodes.isEmpty()) {
            log.info("渠道账号角色已清空: accountCode={}", accountCode);
            return;
        }

        List<String> distinctRoles = roleCodes.stream().distinct().collect(Collectors.toList());
        for (String roleCode : distinctRoles) {
            ChannelAccountRoleRel rel = new ChannelAccountRoleRel();
            rel.setAccountCode(accountCode);
            rel.setRoleCode(roleCode);
            rel.setChannelCode(account.getChannelCode());
            accountRoleRelMapper.insert(rel);
        }
        log.info("渠道账号角色分配完成: accountCode={}, 角色数={}", accountCode, distinctRoles.size());
    }

    @Override
    public List<String> listRoles(String accountCode) {
        List<ChannelAccountRoleRel> rels = accountRoleRelMapper.selectList(
                new LambdaQueryWrapper<ChannelAccountRoleRel>()
                        .eq(ChannelAccountRoleRel::getAccountCode, accountCode));
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        return rels.stream()
                .map(ChannelAccountRoleRel::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
    }
}
