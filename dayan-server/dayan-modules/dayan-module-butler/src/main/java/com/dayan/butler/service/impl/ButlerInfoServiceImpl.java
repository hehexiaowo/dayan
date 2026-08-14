package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerAccountOpenDTO;
import com.dayan.butler.dto.ButlerInfoCreateDTO;
import com.dayan.butler.dto.ButlerInfoQueryDTO;
import com.dayan.butler.dto.ButlerInfoUpdateDTO;
import com.dayan.butler.entity.ButlerInfo;
import com.dayan.butler.mapper.ButlerInfoMapper;
import com.dayan.butler.service.ButlerInfoService;
import com.dayan.butler.vo.ButlerInfoVO;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.organ.entity.OrganAccount;
import com.dayan.organ.entity.OrganEmployee;
import com.dayan.organ.service.OrganAccountRoleService;
import com.dayan.organ.service.OrganAccountService;
import com.dayan.organ.service.OrganEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管家信息服务实现。
 *
 * <p>管家编码生成：{@code "BT" + String.format("%05d", sequenceProvider.next("code:seq:BT:0"))}，
 * 全表唯一。新建默认 status=1（启用）。
 *
 * <p>账号体系：管家保留独立账号（butler_account，面向未来管家端）；同时可开通
 * organ 后台账号（organ_account + organ_employee + ROLE_BUTLER 角色）直接登录 admin，
 * 后台账号编码回写 {@code butler_info.account_code}。管家停用/删除时联动禁用后台账号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerInfoServiceImpl implements ButlerInfoService {

    /** 管家编码前缀 */
    private static final String CODE_PREFIX = "BT";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:BT:0";
    /** 默认状态：启用 */
    private static final int DEFAULT_STATUS = 1;
    /** 平台默认组织（运营方大雁养老） */
    private static final String DEFAULT_ORGAN_CODE = "OR00001";
    /** 养老管家部门编码（40_butler_organ_link.sql 预置） */
    private static final String BUTLER_DEPT_CODE = "DEPT_BUTLER";
    /** 管家默认角色：普通管家 */
    private static final String BUTLER_ROLE_CODE = "ROLE_BUTLER";

    private final ButlerInfoMapper butlerInfoMapper;
    private final SequenceProvider sequenceProvider;
    private final OrganAccountService organAccountService;
    private final OrganEmployeeService organEmployeeService;
    private final OrganAccountRoleService organAccountRoleService;

    @Override
    public PageResult<ButlerInfoVO> page(ButlerInfoQueryDTO query) {
        LambdaQueryWrapper<ButlerInfo> wrapper = buildQueryWrapper(query);
        Page<ButlerInfo> page = butlerInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerInfoVO> list(ButlerInfoQueryDTO query) {
        LambdaQueryWrapper<ButlerInfo> wrapper = buildQueryWrapper(query);
        return butlerInfoMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerInfoVO getDetail(String butlerCode) {
        return toVO(requireButler(butlerCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ButlerInfoCreateDTO dto) {
        String butlerCode = generateButlerCode();

        ButlerInfo entity = new ButlerInfo();
        entity.setButlerCode(butlerCode);
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setAvatar(dto.getAvatar());
        // organ_code 为 NOT NULL 列，未指定所属组织时兜底平台运营方
        entity.setOrganCode(dto.getOrganCode() != null && !dto.getOrganCode().isEmpty()
                ? dto.getOrganCode() : DEFAULT_ORGAN_CODE);
        entity.setButlerLevel(dto.getButlerLevel());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());
        entity.setRemark(dto.getRemark());

        butlerInfoMapper.insert(entity);

        // 填写了 username 则同步开通 organ 后台账号
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            String accountCode = provisionAccount(entity, dto.getUsername(), dto.getPassword());
            entity.setAccountCode(accountCode);
        }
        log.info("创建管家成功: butlerCode={}, fullName={}, accountCode={}",
                butlerCode, dto.getFullName(), entity.getAccountCode());
        return butlerCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String butlerCode, ButlerInfoUpdateDTO dto) {
        ButlerInfo existing = requireButler(butlerCode);
        ButlerInfo update = new ButlerInfo();
        update.setId(existing.getId());

        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) update.setAvatar(dto.getAvatar());
        if (dto.getOrganCode() != null) update.setOrganCode(dto.getOrganCode());
        if (dto.getButlerLevel() != null) update.setButlerLevel(dto.getButlerLevel());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        butlerInfoMapper.updateById(update);

        // 已开通后台账号的管家：基础资料同步 + 停用联动禁用账号
        if (existing.getAccountCode() != null && !existing.getAccountCode().isEmpty()) {
            syncAccountProfile(existing, dto);
            if (dto.getStatus() != null) {
                switchAccountStatusQuietly(existing.getAccountCode(),
                        dto.getStatus() == DEFAULT_STATUS ? 1 : 2);
            }
        }
        log.info("更新管家成功: butlerCode={}", butlerCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String openAccount(String butlerCode, ButlerAccountOpenDTO dto) {
        ButlerInfo existing = requireButler(butlerCode);
        if (existing.getAccountCode() != null && !existing.getAccountCode().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "管家已开通后台账号: " + existing.getAccountCode());
        }
        String accountCode = provisionAccount(existing, dto.getUsername(), dto.getPassword());
        ButlerInfo update = new ButlerInfo();
        update.setId(existing.getId());
        update.setAccountCode(accountCode);
        butlerInfoMapper.updateById(update);
        log.info("开通管家后台账号: butlerCode={}, accountCode={}", butlerCode, accountCode);
        return accountCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String butlerCode) {
        ButlerInfo existing = requireButler(butlerCode);
        // 后台账号保留（登录审计需要），仅禁用
        if (existing.getAccountCode() != null && !existing.getAccountCode().isEmpty()) {
            switchAccountStatusQuietly(existing.getAccountCode(), 2);
        }
        butlerInfoMapper.deleteById(existing.getId());
        log.info("删除管家成功: butlerCode={}", butlerCode);
    }

    // ====== 内部方法 ======

    /**
     * 开通 organ 后台账号：organ_account + organ_employee（养老管家部门）+ ROLE_BUTLER 角色。
     *
     * @return organ_account.account_code
     */
    private String provisionAccount(ButlerInfo butler, String username, String rawPassword) {
        OrganAccount account = new OrganAccount();
        account.setOrganCode(butler.getOrganCode());
        account.setUsername(username);
        account.setPassword(rawPassword);
        account.setRealName(butler.getFullName());
        account.setPhone(butler.getPhone());
        account.setAvatar(butler.getAvatar());
        account.setRemark("管家后台账号（butlerCode=" + butler.getButlerCode() + "）");
        String accountCode = organAccountService.create(account);

        // 员工档案：工号沿用 butlerCode，挂靠养老管家部门（默认组织才有该预置部门）
        OrganEmployee employee = new OrganEmployee();
        employee.setOrganCode(butler.getOrganCode());
        employee.setEmployeeCode(butler.getButlerCode());
        employee.setAccountCode(accountCode);
        if (DEFAULT_ORGAN_CODE.equals(butler.getOrganCode())) {
            employee.setDeptCode(BUTLER_DEPT_CODE);
        }
        employee.setRealName(butler.getFullName());
        employee.setPhone(butler.getPhone());
        employee.setAvatar(butler.getAvatar());
        employee.setPosition("养老管家");
        employee.setEmployeeStatus(butler.getStatus() != null && butler.getStatus() == DEFAULT_STATUS ? 1 : 0);
        employee.setRemark("管家档案（butlerCode=" + butler.getButlerCode() + "）");
        organEmployeeService.create(employee);

        organAccountRoleService.assignRoles(accountCode, List.of(BUTLER_ROLE_CODE));
        return accountCode;
    }

    /** 管家基础资料变更同步到后台账号与员工档案。 */
    private void syncAccountProfile(ButlerInfo existing, ButlerInfoUpdateDTO dto) {
        if (dto.getFullName() == null && dto.getPhone() == null && dto.getAvatar() == null) {
            return;
        }
        String accountCode = existing.getAccountCode();
        OrganAccount account = new OrganAccount();
        if (dto.getFullName() != null) account.setRealName(dto.getFullName());
        if (dto.getPhone() != null) account.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) account.setAvatar(dto.getAvatar());
        try {
            organAccountService.update(accountCode, account);
            OrganEmployee employee = new OrganEmployee();
            if (dto.getFullName() != null) employee.setRealName(dto.getFullName());
            if (dto.getPhone() != null) employee.setPhone(dto.getPhone());
            if (dto.getAvatar() != null) employee.setAvatar(dto.getAvatar());
            organEmployeeService.update(existing.getOrganCode(), existing.getButlerCode(), employee);
        } catch (BusinessException e) {
            // 后台账号/员工档案被单独删除（数据漂移）不阻断管家资料更新
            log.warn("同步管家后台账号资料失败（已跳过）: butlerCode={}, msg={}", existing.getButlerCode(), e.getMessage());
        }
    }

    /** 联动切换后台账号状态：账号被单独删除时仅告警不阻断。 */
    private void switchAccountStatusQuietly(String accountCode, int status) {
        try {
            organAccountService.switchStatus(accountCode, status);
        } catch (BusinessException e) {
            log.warn("联动切换管家后台账号状态失败（已跳过）: accountCode={}, msg={}", accountCode, e.getMessage());
        }
    }

    private LambdaQueryWrapper<ButlerInfo> buildQueryWrapper(ButlerInfoQueryDTO query) {
        return new LambdaQueryWrapper<ButlerInfo>()
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerInfo::getButlerCode, query.getButlerCode())
                .like(query.getFullName() != null && !query.getFullName().isEmpty(),
                        ButlerInfo::getFullName, query.getFullName())
                .eq(query.getPhone() != null && !query.getPhone().isEmpty(),
                        ButlerInfo::getPhone, query.getPhone())
                .eq(query.getOrganCode() != null && !query.getOrganCode().isEmpty(),
                        ButlerInfo::getOrganCode, query.getOrganCode())
                .eq(query.getButlerLevel() != null,
                        ButlerInfo::getButlerLevel, query.getButlerLevel())
                .eq(query.getStatus() != null, ButlerInfo::getStatus, query.getStatus())
                .orderByDesc(ButlerInfo::getCreatedAt);
    }

    ButlerInfo requireButler(String butlerCode) {
        ButlerInfo butler = butlerInfoMapper.selectOne(new LambdaQueryWrapper<ButlerInfo>()
                .eq(ButlerInfo::getButlerCode, butlerCode)
                .last("LIMIT 1"));
        if (butler == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家不存在: " + butlerCode);
        }
        return butler;
    }

    /** 生成管家编码：BT + 5 位序列 */
    private String generateButlerCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private ButlerInfoVO toVO(ButlerInfo entity) {
        ButlerInfoVO vo = new ButlerInfoVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setFullName(entity.getFullName());
        vo.setPhone(entity.getPhone());
        vo.setAvatar(entity.getAvatar());
        vo.setOrganCode(entity.getOrganCode());
        vo.setAccountCode(entity.getAccountCode());
        vo.setButlerLevel(entity.getButlerLevel());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
