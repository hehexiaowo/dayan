package com.dayan.equity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.crypto.AesGcmUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.equity.dto.EquityUsePersonCreateDTO;
import com.dayan.equity.dto.EquityUsePersonQueryDTO;
import com.dayan.equity.dto.EquityUsePersonUpdateDTO;
import com.dayan.equity.dto.SetDefaultHolderDTO;
import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.entity.EquityUsePerson;
import com.dayan.equity.mapper.EquityDepotMapper;
import com.dayan.equity.mapper.EquityUsePersonMapper;
import com.dayan.equity.service.EquityUsePersonService;
import com.dayan.equity.vo.EquityUsePersonVO;
import com.dayan.common.security.secret.DayanSecrets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权益使用人服务实现。
 *
 * <p>身份证加密：AES-256-GCM（每次 IV 随机，同明文密文不同）。
 * 唯一校验改为：查同 equity_code 下所有使用人解密后比对（使用人 ≤3，性能可接受）。
 *
 * <p>注意：AES 密钥由 {@link com.dayan.common.security.secret.DayanSecrets} 单点提供，
 * 需显式构造器注入，未使用 {@code @RequiredArgsConstructor}。
 */
@Slf4j
@Service
public class EquityUsePersonServiceImpl implements EquityUsePersonService {

    /** 使用人人数兜底（depot 快照缺失时） */
    private static final int DEFAULT_PERSON_COUNT = 1;
    private final EquityUsePersonMapper usePersonMapper;
    private final EquityDepotMapper depotMapper;
    private final String aesKeyHex;

    public EquityUsePersonServiceImpl(
            EquityUsePersonMapper usePersonMapper,
            EquityDepotMapper depotMapper,
            DayanSecrets dayanSecrets) {
        this.usePersonMapper = usePersonMapper;
        this.depotMapper = depotMapper;
        this.aesKeyHex = dayanSecrets.aesKeyHex();
    }

    @Override
    public PageResult<EquityUsePersonVO> page(EquityUsePersonQueryDTO query) {
        LambdaQueryWrapper<EquityUsePerson> wrapper = buildQueryWrapper(query);
        Page<EquityUsePerson> page = usePersonMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<EquityUsePersonVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<EquityUsePersonVO> listByEquity(String equityCode) {
        return usePersonMapper.selectList(new LambdaQueryWrapper<EquityUsePerson>()
                .eq(EquityUsePerson::getEquityCode, equityCode)
                .orderByDesc(EquityUsePerson::getIsDefaultHolder)
                .orderByDesc(EquityUsePerson::getCreatedAt))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public EquityUsePersonVO getDetail(Long id) {
        return toVO(requireUsePerson(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(EquityUsePersonCreateDTO dto) {
        // 1. 数量限制 = depot.personCount 快照
        int maxPersons = getPersonCountLimit(dto.getEquityCode());
        Long count = usePersonMapper.selectCount(new LambdaQueryWrapper<EquityUsePerson>()
                .eq(EquityUsePerson::getEquityCode, dto.getEquityCode()));
        if (count != null && count >= maxPersons) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "单权益使用人已达上限 " + maxPersons + " 人");
        }

        // 2. 关系合法性 + 构成规则校验（holder_rule：配偶≤1、父母≤4、本人唯一）
        validateRelationAgainstRule(dto.getEquityCode(), dto.getRelationWithHolder(), null);

        // 3. 身份证唯一校验（解密比对）
        if (dto.getUsePersonIdCard() != null && !dto.getUsePersonIdCard().isEmpty()) {
            checkIdCardUnique(dto.getEquityCode(), dto.getUsePersonIdCard(), null);
        }

        EquityUsePerson entity = new EquityUsePerson();
        entity.setEquityCode(dto.getEquityCode());
        entity.setClientCode(dto.getClientCode());
        entity.setUsePersonName(dto.getUsePersonName());
        entity.setUsePersonGender(dto.getUsePersonGender());
        entity.setUsePersonBirthday(dto.getUsePersonBirthday());
        entity.setUsePersonAge(dto.getUsePersonAge());
        entity.setUsePersonPhone(dto.getUsePersonPhone());
        entity.setUsePersonIdCard(encryptIdCard(dto.getUsePersonIdCard()));
        entity.setRelationWithHolder(dto.getRelationWithHolder());
        entity.setHealthStatus(dto.getHealthStatus());
        entity.setCareNeed(dto.getCareNeed());
        entity.setIsDefaultHolder(dto.getIsDefaultHolder() == null ? 0 : dto.getIsDefaultHolder());
        entity.setRemark(dto.getRemark());

        // 设默认 → 旧的置 0
        if (entity.getIsDefaultHolder() == 1) {
            clearOtherDefault(dto.getEquityCode());
        }

        usePersonMapper.insert(entity);
        log.info("创建权益使用人成功: equityCode={}, id={}, isDefault={}",
                dto.getEquityCode(), entity.getId(), entity.getIsDefaultHolder());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, EquityUsePersonUpdateDTO dto) {
        EquityUsePerson existing = requireUsePerson(id);
        EquityUsePerson update = new EquityUsePerson();
        update.setId(id);

        if (dto.getUsePersonName() != null) update.setUsePersonName(dto.getUsePersonName());
        if (dto.getUsePersonGender() != null) update.setUsePersonGender(dto.getUsePersonGender());
        if (dto.getUsePersonBirthday() != null) update.setUsePersonBirthday(dto.getUsePersonBirthday());
        if (dto.getUsePersonAge() != null) update.setUsePersonAge(dto.getUsePersonAge());
        if (dto.getUsePersonPhone() != null) update.setUsePersonPhone(dto.getUsePersonPhone());
        if (dto.getRelationWithHolder() != null) {
            // 关系变更时校验构成规则（排除自身计数）
            validateRelationAgainstRule(existing.getEquityCode(), dto.getRelationWithHolder(), id);
            update.setRelationWithHolder(dto.getRelationWithHolder());
        }
        if (dto.getHealthStatus() != null) update.setHealthStatus(dto.getHealthStatus());
        if (dto.getCareNeed() != null) update.setCareNeed(dto.getCareNeed());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        if (dto.getUsePersonIdCard() != null && !dto.getUsePersonIdCard().isEmpty()) {
            // 唯一校验排除自身
            checkIdCardUnique(existing.getEquityCode(), dto.getUsePersonIdCard(), id);
            update.setUsePersonIdCard(encryptIdCard(dto.getUsePersonIdCard()));
        }

        usePersonMapper.updateById(update);
        log.info("更新权益使用人成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        EquityUsePerson existing = requireUsePerson(id);
        usePersonMapper.deleteById(id);
        log.info("删除权益使用人: id={}, equityCode={}, wasDefault={}",
                id, existing.getEquityCode(), existing.getIsDefaultHolder());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(SetDefaultHolderDTO dto) {
        EquityUsePerson target = requireUsePerson(dto.getId());
        if (!target.getEquityCode().equals(dto.getEquityCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "使用人不属于该权益");
        }
        // 旧的置 0
        clearOtherDefault(dto.getEquityCode());
        // 目标置 1
        usePersonMapper.update(null, new LambdaUpdateWrapper<EquityUsePerson>()
                .eq(EquityUsePerson::getId, dto.getId())
                .set(EquityUsePerson::getIsDefaultHolder, 1));
        log.info("设置默认权益人: equityCode={}, id={}", dto.getEquityCode(), dto.getId());
    }

    // ====== 内部方法 ======

    /**
     * 从 depot 快照取使用人人数上限（depot.personCount）。
     */
    private int getPersonCountLimit(String equityCode) {
        EquityDepot depot = depotMapper.selectOne(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getEquityCode, equityCode)
                .last("LIMIT 1"));
        if (depot != null && depot.getPersonCount() != null && depot.getPersonCount() > 0) {
            return depot.getPersonCount();
        }
        return DEFAULT_PERSON_COUNT;
    }

    /** 合法的与持有人关系字典code（system_dict: relation_with_holder） */
    private static final Set<String> VALID_RELATIONS =
            Set.of("self", "spouse", "parent", "parent_in_law", "child", "other");

    /**
     * 权益人关系校验：
     * ① 必须是字典合法值；② 有 holder_rule 快照时按构成规则校验席位——
     * 本人唯一、配偶 ≤ 规则配偶席位、父母（含公婆/岳父母）合计 ≤ 规则父母席位。
     * 无规则快照（旧数据）时仅做字典值校验。
     *
     * @param equityCode    权益编码
     * @param relation      待写入的关系code
     * @param excludePersonId 更新场景排除自身（null=新增场景）
     */
    private void validateRelationAgainstRule(String equityCode, String relation, Long excludePersonId) {
        if (relation == null || !VALID_RELATIONS.contains(relation)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "与持有人关系不合法: " + relation + "（须为 self/spouse/parent/parent_in_law/child/other）");
        }
        EquityDepot depot = depotMapper.selectOne(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getEquityCode, equityCode)
                .last("LIMIT 1"));
        if (depot == null) {
            return;
        }
        com.dayan.goods.model.HolderRule rule = com.dayan.goods.model.RightsJson.read(
                depot.getHolderRule(), com.dayan.goods.model.HolderRule.class);
        if (rule == null) {
            return;
        }

        List<EquityUsePerson> all = usePersonMapper.selectList(new LambdaQueryWrapper<EquityUsePerson>()
                .eq(EquityUsePerson::getEquityCode, equityCode));
        long spouseCount = 0;
        long parentCount = 0;
        boolean selfExists = false;
        for (EquityUsePerson p : all) {
            if (excludePersonId != null && excludePersonId.equals(p.getId())) {
                continue;
            }
            String rel = p.getRelationWithHolder();
            if ("self".equals(rel)) {
                selfExists = true;
            } else if ("spouse".equals(rel)) {
                spouseCount++;
            } else if ("parent".equals(rel) || "parent_in_law".equals(rel)) {
                parentCount++;
            }
        }

        switch (relation) {
            case "self" -> {
                if (selfExists) {
                    throw new BusinessException(ErrorCode.BUSINESS, "该权益已有本人（默认权益人），不能再添加");
                }
            }
            case "spouse" -> {
                int spouseLimit = rule.getSpouse() == null ? 0 : rule.getSpouse();
                if (spouseCount + 1 > spouseLimit) {
                    throw new BusinessException(ErrorCode.BUSINESS,
                            "该权益的构成不含配偶席位或配偶已存在（上限 " + spouseLimit + " 人）");
                }
            }
            case "parent", "parent_in_law" -> {
                int parentLimit = rule.getParent() == null ? 0 : rule.getParent();
                if (parentCount + 1 > parentLimit) {
                    throw new BusinessException(ErrorCode.BUSINESS,
                            "该权益的父母席位已满（上限 " + parentLimit + " 人，含公婆/岳父母）");
                }
            }
            default -> { /* child/other 不受构成席位约束，仅受总人数上限约束 */ }
        }
    }

    private EquityUsePerson requireUsePerson(Long id) {
        EquityUsePerson entity = usePersonMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益使用人不存在: id=" + id);
        }
        return entity;
    }

    /**
     * 身份证唯一校验：查同 equity_code 下所有使用人，解密后比对（排除 excludeId）。
     * 原因：AES-GCM 每次 IV 随机，同明文密文不同，无法直接用 .eq(加密值) 匹配。
     */
    private void checkIdCardUnique(String equityCode, String plainIdCard, Long excludeId) {
        List<EquityUsePerson> all = usePersonMapper.selectList(new LambdaQueryWrapper<EquityUsePerson>()
                .eq(EquityUsePerson::getEquityCode, equityCode));
        for (EquityUsePerson p : all) {
            if (excludeId != null && excludeId.equals(p.getId())) {
                continue;
            }
            String decrypted = decryptSafely(p.getUsePersonIdCard());
            if (plainIdCard.equals(decrypted)) {
                throw new BusinessException(ErrorCode.BUSINESS, "同权益下身份证号已存在: " + maskIdCard(plainIdCard));
            }
        }
    }

    private void clearOtherDefault(String equityCode) {
        usePersonMapper.update(null, new LambdaUpdateWrapper<EquityUsePerson>()
                .eq(EquityUsePerson::getEquityCode, equityCode)
                .eq(EquityUsePerson::getIsDefaultHolder, 1)
                .set(EquityUsePerson::getIsDefaultHolder, 0));
    }

    private String encryptIdCard(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        return AesGcmUtil.encrypt(plain, aesKeyHex);
    }

    private String decryptSafely(String cipher) {
        if (cipher == null || cipher.isEmpty()) {
            return null;
        }
        try {
            return AesGcmUtil.decrypt(cipher, aesKeyHex);
        } catch (Exception e) {
            return null;
        }
    }

    /** 身份证号脱敏（保留前 6 后 4） */
    private static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return "***";
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    private LambdaQueryWrapper<EquityUsePerson> buildQueryWrapper(EquityUsePersonQueryDTO query) {
        LambdaQueryWrapper<EquityUsePerson> wrapper = new LambdaQueryWrapper<EquityUsePerson>()
                .orderByDesc(EquityUsePerson::getIsDefaultHolder)
                .orderByDesc(EquityUsePerson::getCreatedAt);
        if (query.getEquityCode() != null && !query.getEquityCode().isEmpty()) {
            wrapper.eq(EquityUsePerson::getEquityCode, query.getEquityCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(EquityUsePerson::getClientCode, query.getClientCode());
        }
        if (query.getUsePersonName() != null && !query.getUsePersonName().isEmpty()) {
            wrapper.like(EquityUsePerson::getUsePersonName, query.getUsePersonName());
        }
        if (query.getIsDefaultHolder() != null) {
            wrapper.eq(EquityUsePerson::getIsDefaultHolder, query.getIsDefaultHolder());
        }
        return wrapper;
    }

    private EquityUsePersonVO toVO(EquityUsePerson entity) {
        EquityUsePersonVO vo = new EquityUsePersonVO();
        vo.setId(entity.getId());
        vo.setEquityCode(entity.getEquityCode());
        vo.setClientCode(entity.getClientCode());
        vo.setUsePersonName(entity.getUsePersonName());
        vo.setUsePersonGender(entity.getUsePersonGender());
        vo.setUsePersonBirthday(entity.getUsePersonBirthday());
        vo.setUsePersonAge(entity.getUsePersonAge());
        vo.setUsePersonPhone(entity.getUsePersonPhone());
        // 解密身份证回传（管理端可见）
        vo.setUsePersonIdCard(decryptSafely(entity.getUsePersonIdCard()));
        vo.setRelationWithHolder(entity.getRelationWithHolder());
        vo.setHealthStatus(entity.getHealthStatus());
        vo.setCareNeed(entity.getCareNeed());
        vo.setIsDefaultHolder(entity.getIsDefaultHolder());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
