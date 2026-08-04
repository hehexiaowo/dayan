package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.security.password.PasswordService;
import com.dayan.supplier.dto.SupplierAccountCreateDTO;
import com.dayan.supplier.dto.SupplierAccountQueryDTO;
import com.dayan.supplier.dto.SupplierAccountUpdateDTO;
import com.dayan.supplier.entity.SupplierAccount;
import com.dayan.supplier.mapper.SupplierAccountMapper;
import com.dayan.supplier.service.SupplierAccountService;
import com.dayan.supplier.vo.SupplierAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 供应商账号服务实现。
 *
 * <p>复用 {@link PasswordService} 进行 BCrypt 哈希。主账号（{@code isAdmin=1}）同 supplierCode 下唯一：
 * 设置主账号时先把同 supplierCode 下其他账号 isAdmin 置 0（事务内）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierAccountServiceImpl implements SupplierAccountService {

    /** 重置密码默认值 */
    private static final String DEFAULT_RESET_PASSWORD = "dayan@123";

    private final SupplierAccountMapper accountMapper;
    private final PasswordService passwordService;

    @Override
    public PageResult<SupplierAccountVO> page(SupplierAccountQueryDTO query) {
        LambdaQueryWrapper<SupplierAccount> wrapper = new LambdaQueryWrapper<SupplierAccount>()
                .eq(query.getSupplierCode() != null && !query.getSupplierCode().isEmpty(),
                        SupplierAccount::getSupplierCode, query.getSupplierCode())
                .eq(query.getIsAdmin() != null, SupplierAccount::getIsAdmin, query.getIsAdmin())
                .eq(query.getAccountStatus() != null,
                        SupplierAccount::getAccountStatus, query.getAccountStatus())
                .orderByDesc(SupplierAccount::getCreatedAt);
        if (query.getUsername() != null && !query.getUsername().isEmpty()) {
            wrapper.and(w -> w.eq(SupplierAccount::getUsername, query.getUsername())
                    .or().eq(SupplierAccount::getPhone, query.getUsername())
                    .or().eq(SupplierAccount::getEmail, query.getUsername()));
        }
        if (query.getRealName() != null && !query.getRealName().isEmpty()) {
            wrapper.like(SupplierAccount::getRealName, query.getRealName());
        }
        Page<SupplierAccount> page = accountMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SupplierAccountVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public SupplierAccountVO getDetail(String accountCode) {
        return toVO(selectByCode(accountCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SupplierAccountCreateDTO dto) {
        String supplierCode = dto.getSupplierCode();
        if (supplierCode == null || supplierCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "供应商编码不能为空");
        }

        // username 同 supplierCode 下唯一校验
        Long usernameCount = accountMapper.selectCount(new LambdaQueryWrapper<SupplierAccount>()
                .eq(SupplierAccount::getSupplierCode, supplierCode)
                .eq(SupplierAccount::getUsername, dto.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "用户名已存在: " + dto.getUsername());
        }

        // 若标记为主账号，先把同 supplierCode 下其他账号 isAdmin 置 0
        Integer isAdmin = dto.getIsAdmin() == null ? 0 : dto.getIsAdmin();
        if (isAdmin == 1) {
            clearOtherAdmin(supplierCode);
        }

        SupplierAccount entity = new SupplierAccount();
        entity.setSupplierCode(supplierCode);
        entity.setAccountCode(generateAccountCode());
        entity.setUsername(dto.getUsername());
        String rawPwd = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? DEFAULT_RESET_PASSWORD : dto.getPassword();
        entity.setPassword(passwordService.encode(rawPwd));
        entity.setSalt("bcrypt");
        entity.setRealName(dto.getRealName());
        entity.setAvatar(dto.getAvatar());
        entity.setPhone(dto.getPhone());
        entity.setOpenId(dto.getOpenId());
        entity.setUnionId(dto.getUnionId());
        entity.setEmail(dto.getEmail());
        entity.setPosition(dto.getPosition());
        entity.setLoginCount(0);
        entity.setAccountStatus(dto.getAccountStatus() == null ? 1 : dto.getAccountStatus());
        entity.setIsAdmin(isAdmin);
        accountMapper.insert(entity);
        log.info("创建供应商账号成功: accountCode={}, supplierCode={}, username={}",
                entity.getAccountCode(), supplierCode, entity.getUsername());
        return entity.getAccountCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String accountCode, SupplierAccountUpdateDTO dto) {
        SupplierAccount existing = selectByCode(accountCode);
        SupplierAccount update = new SupplierAccount();
        update.setId(existing.getId());

        if (dto.getRealName() != null) update.setRealName(dto.getRealName());
        if (dto.getAvatar() != null) update.setAvatar(dto.getAvatar());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getOpenId() != null) update.setOpenId(dto.getOpenId());
        if (dto.getUnionId() != null) update.setUnionId(dto.getUnionId());
        if (dto.getEmail() != null) update.setEmail(dto.getEmail());
        if (dto.getPosition() != null) update.setPosition(dto.getPosition());
        if (dto.getAccountStatus() != null) update.setAccountStatus(dto.getAccountStatus());

        if (dto.getIsAdmin() != null) {
            // 设为主账号时，先把同 supplierCode 下其他账号 isAdmin 置 0
            if (dto.getIsAdmin() == 1) {
                clearOtherAdmin(existing.getSupplierCode());
            }
            update.setIsAdmin(dto.getIsAdmin());
        }
        accountMapper.updateById(update);
        log.info("更新供应商账号成功: accountCode={}", accountCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String accountCode) {
        SupplierAccount existing = selectByCode(accountCode);
        SupplierAccount update = new SupplierAccount();
        update.setId(existing.getId());
        update.setPassword(passwordService.encode(DEFAULT_RESET_PASSWORD));
        accountMapper.updateById(update);
        log.info("重置供应商账号密码: accountCode={}", accountCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String accountCode) {
        SupplierAccount existing = selectByCode(accountCode);
        if (existing.getIsAdmin() != null && existing.getIsAdmin() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "供应商管理员账号不可删除");
        }
        accountMapper.deleteById(existing.getId());
        log.info("删除供应商账号成功: accountCode={}", accountCode);
    }

    // ====== 内部方法 ======

    private SupplierAccount selectByCode(String accountCode) {
        SupplierAccount account = accountMapper.selectOne(new LambdaQueryWrapper<SupplierAccount>()
                .eq(SupplierAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商账号不存在: " + accountCode);
        }
        return account;
    }

    /** 把同 supplierCode 下所有账号 isAdmin 置 0 */
    private void clearOtherAdmin(String supplierCode) {
        accountMapper.update(null, new LambdaUpdateWrapper<SupplierAccount>()
                .eq(SupplierAccount::getSupplierCode, supplierCode)
                .eq(SupplierAccount::getIsAdmin, 1)
                .set(SupplierAccount::getIsAdmin, 0));
    }

    /** 简易账号编码生成：SA + 时间戳后 5 位 + 随机 3 位（唯一性由 DB 索引保障） */
    private String generateAccountCode() {
        long ts = System.currentTimeMillis() % 100000L;
        int rand = (int) (Math.random() * 1000);
        return String.format("SA%05d%03d", ts, rand);
    }

    private SupplierAccountVO toVO(SupplierAccount entity) {
        SupplierAccountVO vo = new SupplierAccountVO();
        vo.setId(entity.getId());
        vo.setSupplierCode(entity.getSupplierCode());
        vo.setAccountCode(entity.getAccountCode());
        vo.setUsername(entity.getUsername());
        vo.setRealName(entity.getRealName());
        vo.setAvatar(entity.getAvatar());
        vo.setPhone(entity.getPhone());
        vo.setOpenId(entity.getOpenId());
        vo.setUnionId(entity.getUnionId());
        vo.setEmail(entity.getEmail());
        vo.setPosition(entity.getPosition());
        vo.setLastLoginTime(entity.getLastLoginTime());
        vo.setLastLoginIp(entity.getLastLoginIp());
        vo.setLoginCount(entity.getLoginCount());
        vo.setPwdUpdateTime(entity.getPwdUpdateTime());
        vo.setAccountStatus(entity.getAccountStatus());
        vo.setIsAdmin(entity.getIsAdmin());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
