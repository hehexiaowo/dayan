package com.dayan.organ.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.organ.entity.OrganAccount;
import com.dayan.organ.entity.OrganAccountRoleRel;
import com.dayan.organ.mapper.OrganAccountMapper;
import com.dayan.organ.mapper.OrganAccountRoleRelMapper;
import com.dayan.organ.service.OrganAccountRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * organ 域账号-角色关联服务实现。
 *
 * <p>分配采用"先删后增"全量覆盖语义；关联记录的 organ_code 从账号本身取值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganAccountRoleServiceImpl implements OrganAccountRoleService {

    private final OrganAccountMapper accountMapper;
    private final OrganAccountRoleRelMapper accountRoleRelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(String accountCode, List<String> roleCodes) {
        // 校验账号存在，并取 organ_code
        OrganAccount account = accountMapper.selectOne(new LambdaQueryWrapper<OrganAccount>()
                .eq(OrganAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在：" + accountCode);
        }

        // 先删后增（全量覆盖）：物理删除避免逻辑删除残留占 uk_account_role 唯一键
        accountRoleRelMapper.physicallyDeleteByAccountCode(accountCode);

        if (roleCodes == null || roleCodes.isEmpty()) {
            log.info("账号角色已清空: accountCode={}", accountCode);
            return;
        }

        List<String> distinctRoles = roleCodes.stream().distinct().collect(Collectors.toList());
        for (String roleCode : distinctRoles) {
            OrganAccountRoleRel rel = new OrganAccountRoleRel();
            rel.setAccountCode(accountCode);
            rel.setRoleCode(roleCode);
            rel.setOrganCode(account.getOrganCode());
            accountRoleRelMapper.insert(rel);
        }
        log.info("账号角色分配完成: accountCode={}, 角色数={}", accountCode, distinctRoles.size());
    }

    @Override
    public List<String> listRoles(String accountCode) {
        List<OrganAccountRoleRel> rels = accountRoleRelMapper.selectList(
                new LambdaQueryWrapper<OrganAccountRoleRel>()
                        .eq(OrganAccountRoleRel::getAccountCode, accountCode));
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        return rels.stream()
                .map(OrganAccountRoleRel::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
    }
}
