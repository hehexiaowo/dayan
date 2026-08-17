package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.channel.dto.ChannelAccountCreateDTO;
import com.dayan.channel.dto.ChannelAccountQueryDTO;
import com.dayan.channel.dto.ChannelAccountUpdateDTO;
import com.dayan.channel.entity.ChannelAccount;
import com.dayan.channel.entity.ChannelAccountRoleRel;
import com.dayan.channel.entity.ChannelRole;
import com.dayan.channel.mapper.ChannelAccountMapper;
import com.dayan.channel.mapper.ChannelAccountRoleRelMapper;
import com.dayan.channel.mapper.ChannelRoleMapper;
import com.dayan.channel.service.ChannelAccountService;
import com.dayan.channel.vo.ChannelAccountVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.common.security.password.PasswordService;
import com.dayan.common.security.secret.DayanSecrets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 渠道账号（channel_account）服务实现。
 *
 * <p>复用 {@link PasswordService} 进行 BCrypt 哈希；
 * 渠道隔离由 {@code TenantLineInnerInterceptor} 自动追加 channel_code 条件，
 * Service 层通过显式 {@code channelCode} 参数或 {@link ContextHolder} 兜底确定归属渠道。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelAccountServiceImpl implements ChannelAccountService {

    /** 重置密码默认值：由 DayanSecrets 单点配置（生产必须显式配置） */
    private final DayanSecrets dayanSecrets;

    private final ChannelAccountMapper accountMapper;
    private final ChannelAccountRoleRelMapper accountRoleRelMapper;
    private final ChannelRoleMapper roleMapper;
    private final PasswordService passwordService;

    @Override
    public PageResult<ChannelAccountVO> page(ChannelAccountQueryDTO query) {
        LambdaQueryWrapper<ChannelAccount> wrapper = new LambdaQueryWrapper<ChannelAccount>()
                .orderByDesc(ChannelAccount::getCreatedAt);
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(ChannelAccount::getChannelCode, query.getChannelCode());
        }
        if (query.getUsername() != null && !query.getUsername().isEmpty()) {
            wrapper.and(w -> w.eq(ChannelAccount::getUsername, query.getUsername())
                    .or().eq(ChannelAccount::getPhone, query.getUsername())
                    .or().eq(ChannelAccount::getEmail, query.getUsername()));
        }
        if (query.getRealName() != null && !query.getRealName().isEmpty()) {
            wrapper.like(ChannelAccount::getRealName, query.getRealName());
        }
        if (query.getPhone() != null && !query.getPhone().isEmpty()) {
            wrapper.like(ChannelAccount::getPhone, query.getPhone());
        }
        if (query.getAccountStatus() != null) {
            wrapper.eq(ChannelAccount::getAccountStatus, query.getAccountStatus());
        }
        Page<ChannelAccount> page = accountMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        // 批量回填账号角色名（account → roleCodes → roleNames，避免 N+1）
        List<String> accountCodes = page.getRecords().stream()
                .map(ChannelAccount::getAccountCode)
                .filter(c -> c != null && !c.isEmpty())
                .toList();
        Map<String, List<String>> accountRoles = accountCodes.isEmpty()
                ? Collections.emptyMap() : listAccountRoleCodes(accountCodes);
        Map<String, String> roleNameMap = resolveRoleNames(accountRoles.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet()));
        List<ChannelAccountVO> records = page.getRecords().stream()
                .map(a -> toVO(a, accountRoles, roleNameMap)).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    /** 批量查询账号 → 已分配角色编码列表 */
    private Map<String, List<String>> listAccountRoleCodes(Collection<String> accountCodes) {
        List<ChannelAccountRoleRel> rels = accountRoleRelMapper.selectList(
                new LambdaQueryWrapper<ChannelAccountRoleRel>()
                        .in(ChannelAccountRoleRel::getAccountCode, accountCodes));
        return rels.stream().collect(Collectors.groupingBy(
                ChannelAccountRoleRel::getAccountCode,
                Collectors.mapping(ChannelAccountRoleRel::getRoleCode, Collectors.toList())));
    }

    /** 批量解析 roleCode → roleName */
    private Map<String, String> resolveRoleNames(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ChannelRole> roles = roleMapper.selectList(new LambdaQueryWrapper<ChannelRole>()
                .in(ChannelRole::getRoleCode, roleCodes));
        return roles.stream().collect(Collectors.toMap(
                ChannelRole::getRoleCode, ChannelRole::getRoleName, (x, y) -> x));
    }

    private ChannelAccountVO toVO(ChannelAccount entity, Map<String, List<String>> accountRoles,
                                  Map<String, String> roleNameMap) {
        ChannelAccountVO vo = new ChannelAccountVO();
        vo.setId(entity.getId());
        vo.setChannelCode(entity.getChannelCode());
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
        vo.setAccountStatus(entity.getAccountStatus());
        vo.setIsAdmin(entity.getIsAdmin());
        vo.setCreatedAt(entity.getCreatedAt());
        List<String> roleCodes = accountRoles.getOrDefault(entity.getAccountCode(), Collections.emptyList());
        vo.setRoleNames(roleCodes.stream().map(roleNameMap::get)
                .filter(Objects::nonNull).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public ChannelAccountVO getDetail(String accountCode) {
        return toVO(selectByCode(accountCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ChannelAccountCreateDTO dto) {
        String channelCode = pickChannelCode(dto.getChannelCode());

        // username 渠道内唯一校验
        Long usernameCount = accountMapper.selectCount(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getChannelCode, channelCode)
                .eq(ChannelAccount::getUsername, dto.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "用户名已存在: " + dto.getUsername());
        }

        ChannelAccount entity = new ChannelAccount();
        entity.setChannelCode(channelCode);
        entity.setAccountCode(generateAccountCode());
        entity.setUsername(dto.getUsername());
        String rawPwd = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? dayanSecrets.getDefaultResetPassword() : dto.getPassword();
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
        entity.setIsAdmin(dto.getIsAdmin() == null ? 0 : dto.getIsAdmin());
        accountMapper.insert(entity);
        log.info("创建渠道账号成功: accountCode={}, channelCode={}, username={}",
                entity.getAccountCode(), channelCode, entity.getUsername());
        return entity.getAccountCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String accountCode, ChannelAccountUpdateDTO dto) {
        ChannelAccount existing = selectByCode(accountCode);
        ChannelAccount update = new ChannelAccount();
        update.setId(existing.getId());
        if (dto.getRealName() != null) update.setRealName(dto.getRealName());
        if (dto.getAvatar() != null) update.setAvatar(dto.getAvatar());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getOpenId() != null) update.setOpenId(dto.getOpenId());
        if (dto.getUnionId() != null) update.setUnionId(dto.getUnionId());
        if (dto.getEmail() != null) update.setEmail(dto.getEmail());
        if (dto.getPosition() != null) update.setPosition(dto.getPosition());
        if (dto.getAccountStatus() != null) update.setAccountStatus(dto.getAccountStatus());
        if (dto.getIsAdmin() != null) update.setIsAdmin(dto.getIsAdmin());
        accountMapper.updateById(update);
        log.info("更新渠道账号成功: accountCode={}", accountCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String accountCode) {
        ChannelAccount existing = selectByCode(accountCode);
        ChannelAccount update = new ChannelAccount();
        update.setId(existing.getId());
        update.setPassword(passwordService.encode(dayanSecrets.getDefaultResetPassword()));
        accountMapper.updateById(update);
        log.info("重置渠道账号密码: accountCode={}", accountCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String accountCode) {
        ChannelAccount existing = selectByCode(accountCode);
        if (existing.getIsAdmin() != null && existing.getIsAdmin() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "渠道管理员账号不可删除");
        }
        accountMapper.deleteById(existing.getId());
        log.info("删除渠道账号成功: accountCode={}", accountCode);
    }

    // ====== 内部方法 ======

    private ChannelAccount selectByCode(String accountCode) {
        ChannelAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "渠道账号不存在: " + accountCode);
        }
        return account;
    }

    private String pickChannelCode(String fromDto) {
        if (fromDto != null && !fromDto.isEmpty()) {
            return fromDto;
        }
        String ctx = ContextHolder.getChannelCode();
        if (ctx == null || ctx.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "渠道编码不能为空");
        }
        return ctx;
    }

    /** 简易账号编码生成：CA + 时间戳后 5 位 + 随机 3 位（渠道内唯一性由 DB 索引保障） */
    private String generateAccountCode() {
        long ts = System.currentTimeMillis() % 100000L;
        int rand = (int) (Math.random() * 1000);
        return String.format("CA%05d%03d", ts, rand);
    }

    private ChannelAccountVO toVO(ChannelAccount entity) {
        ChannelAccountVO vo = new ChannelAccountVO();
        vo.setId(entity.getId());
        vo.setChannelCode(entity.getChannelCode());
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
        vo.setAccountStatus(entity.getAccountStatus());
        vo.setIsAdmin(entity.getIsAdmin());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
