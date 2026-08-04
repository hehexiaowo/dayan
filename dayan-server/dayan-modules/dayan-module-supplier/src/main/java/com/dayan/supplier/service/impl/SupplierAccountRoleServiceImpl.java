package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.supplier.entity.SupplierAccount;
import com.dayan.supplier.entity.SupplierAccountRoleRel;
import com.dayan.supplier.mapper.SupplierAccountMapper;
import com.dayan.supplier.mapper.SupplierAccountRoleRelMapper;
import com.dayan.supplier.service.SupplierAccountRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商账号-角色关联服务实现。
 *
 * <p>分配采用"先删后增"全量覆盖语义；关联记录的 supplierCode 从账号本身取值，
 * 确保跨供应商关联记录不会错位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierAccountRoleServiceImpl implements SupplierAccountRoleService {

    private final SupplierAccountMapper accountMapper;
    private final SupplierAccountRoleRelMapper accountRoleRelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(String accountCode, List<String> roleCodes) {
        // 校验账号存在，并取 supplierCode
        SupplierAccount account = accountMapper.selectOne(new LambdaQueryWrapper<SupplierAccount>()
                .eq(SupplierAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商账号不存在: " + accountCode);
        }

        // 先删后增（全量覆盖）
        accountRoleRelMapper.delete(new LambdaQueryWrapper<SupplierAccountRoleRel>()
                .eq(SupplierAccountRoleRel::getAccountCode, accountCode));

        if (roleCodes == null || roleCodes.isEmpty()) {
            log.info("供应商账号角色已清空: accountCode={}", accountCode);
            return;
        }

        List<String> distinctRoles = roleCodes.stream().distinct().collect(Collectors.toList());
        for (String roleCode : distinctRoles) {
            SupplierAccountRoleRel rel = new SupplierAccountRoleRel();
            rel.setAccountCode(accountCode);
            rel.setRoleCode(roleCode);
            rel.setSupplierCode(account.getSupplierCode());
            accountRoleRelMapper.insert(rel);
        }
        log.info("供应商账号角色分配完成: accountCode={}, 角色数={}", accountCode, distinctRoles.size());
    }

    @Override
    public List<String> listRoles(String accountCode) {
        List<SupplierAccountRoleRel> rels = accountRoleRelMapper.selectList(
                new LambdaQueryWrapper<SupplierAccountRoleRel>()
                        .eq(SupplierAccountRoleRel::getAccountCode, accountCode));
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        return rels.stream()
                .map(SupplierAccountRoleRel::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
    }
}
