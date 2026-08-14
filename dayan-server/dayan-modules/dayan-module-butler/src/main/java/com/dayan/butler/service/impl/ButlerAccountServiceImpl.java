package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerAccountCreateDTO;
import com.dayan.butler.dto.ButlerAccountQueryDTO;
import com.dayan.butler.dto.ButlerAccountUpdateDTO;
import com.dayan.butler.entity.ButlerAccount;
import com.dayan.butler.mapper.ButlerAccountMapper;
import com.dayan.butler.service.ButlerAccountService;
import com.dayan.butler.vo.ButlerAccountVO;
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

/**
 * 管家账号服务实现。
 *
 * <p>密码经 BCrypt 哈希存储，盐值字段存固定标记 "bcrypt"。重置密码使用默认密码。
 * 同 butlerCode 下 username 唯一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerAccountServiceImpl implements ButlerAccountService {

    /** 重置密码默认值：由 DayanSecrets 单点配置（生产必须显式配置） */
    private final DayanSecrets dayanSecrets;
    /** 默认账号状态：启用 */
    private static final int DEFAULT_STATUS = 1;

    private final ButlerAccountMapper butlerAccountMapper;
    private final PasswordService passwordService;

    @Override
    public PageResult<ButlerAccountVO> page(ButlerAccountQueryDTO query) {
        LambdaQueryWrapper<ButlerAccount> wrapper = buildQueryWrapper(query);
        Page<ButlerAccount> page = butlerAccountMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerAccountVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerAccountVO> list(ButlerAccountQueryDTO query) {
        LambdaQueryWrapper<ButlerAccount> wrapper = buildQueryWrapper(query);
        return butlerAccountMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerAccountVO getDetail(Long id) {
        return toVO(requireAccount(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ButlerAccountCreateDTO dto) {
        String butlerCode = dto.getButlerCode();
        if (butlerCode == null || butlerCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "管家编码不能为空");
        }

        // username 同 butlerCode 下唯一校验
        Long usernameCount = butlerAccountMapper.selectCount(new LambdaQueryWrapper<ButlerAccount>()
                .eq(ButlerAccount::getButlerCode, butlerCode)
                .eq(ButlerAccount::getUsername, dto.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "用户名已存在: " + dto.getUsername());
        }

        ButlerAccount entity = new ButlerAccount();
        entity.setButlerCode(butlerCode);
        entity.setUsername(dto.getUsername());
        String rawPwd = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? dayanSecrets.getDefaultResetPassword() : dto.getPassword();
        entity.setPassword(passwordService.encode(rawPwd));
        entity.setSalt("bcrypt");
        entity.setPhone(dto.getPhone());
        entity.setOpenId(dto.getOpenId());
        entity.setUnionId(dto.getUnionId());
        entity.setAccountStatus(dto.getAccountStatus() == null ? DEFAULT_STATUS : dto.getAccountStatus());

        butlerAccountMapper.insert(entity);
        log.info("创建管家账号成功: id={}, butlerCode={}, username={}",
                entity.getId(), butlerCode, entity.getUsername());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ButlerAccountUpdateDTO dto) {
        ButlerAccount existing = requireAccount(id);
        ButlerAccount update = new ButlerAccount();
        update.setId(existing.getId());

        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getOpenId() != null) update.setOpenId(dto.getOpenId());
        if (dto.getUnionId() != null) update.setUnionId(dto.getUnionId());
        if (dto.getAccountStatus() != null) update.setAccountStatus(dto.getAccountStatus());

        butlerAccountMapper.updateById(update);
        log.info("更新管家账号成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id) {
        ButlerAccount existing = requireAccount(id);
        ButlerAccount update = new ButlerAccount();
        update.setId(existing.getId());
        update.setPassword(passwordService.encode(dayanSecrets.getDefaultResetPassword()));
        butlerAccountMapper.updateById(update);
        log.info("重置管家账号密码: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ButlerAccount existing = requireAccount(id);
        butlerAccountMapper.deleteById(existing.getId());
        log.info("删除管家账号成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ButlerAccount> buildQueryWrapper(ButlerAccountQueryDTO query) {
        return new LambdaQueryWrapper<ButlerAccount>()
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerAccount::getButlerCode, query.getButlerCode())
                .like(query.getUsername() != null && !query.getUsername().isEmpty(),
                        ButlerAccount::getUsername, query.getUsername())
                .eq(query.getPhone() != null && !query.getPhone().isEmpty(),
                        ButlerAccount::getPhone, query.getPhone())
                .eq(query.getAccountStatus() != null,
                        ButlerAccount::getAccountStatus, query.getAccountStatus())
                .orderByDesc(ButlerAccount::getCreatedAt);
    }

    private ButlerAccount requireAccount(Long id) {
        ButlerAccount account = butlerAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家账号不存在: " + id);
        }
        return account;
    }

    private ButlerAccountVO toVO(ButlerAccount entity) {
        ButlerAccountVO vo = new ButlerAccountVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setUsername(entity.getUsername());
        vo.setPhone(entity.getPhone());
        vo.setOpenId(entity.getOpenId());
        vo.setUnionId(entity.getUnionId());
        vo.setLastLoginTime(entity.getLastLoginTime());
        vo.setAccountStatus(entity.getAccountStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
