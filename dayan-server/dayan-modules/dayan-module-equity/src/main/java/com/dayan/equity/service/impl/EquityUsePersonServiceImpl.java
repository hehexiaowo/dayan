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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权益使用人服务实现。
 *
 * <p>身份证加密：AES-256-GCM（每次 IV 随机，同明文密文不同）。
 * 唯一校验改为：查同 equity_code 下所有使用人解密后比对（使用人 ≤3，性能可接受）。
 *
 * <p>未使用 {@code @RequiredArgsConstructor}：因 AES 密钥需由 {@code @Value} 派生，改用显式构造器。
 */
@Slf4j
@Service
public class EquityUsePersonServiceImpl implements EquityUsePersonService {

    /** 使用人人数兜底（depot 快照缺失时） */
    private static final int DEFAULT_PERSON_COUNT = 1;
    private static final String DEFAULT_KEY_PASSWORD = "dayan-default-key";

    private final EquityUsePersonMapper usePersonMapper;
    private final EquityDepotMapper depotMapper;
    private final String aesKeyHex;

    public EquityUsePersonServiceImpl(
            EquityUsePersonMapper usePersonMapper,
            EquityDepotMapper depotMapper,
            @Value("${dayan.aes.key:}") String configuredKey) {
        this.usePersonMapper = usePersonMapper;
        this.depotMapper = depotMapper;
        if (configuredKey == null || configuredKey.isBlank()) {
            this.aesKeyHex = AesGcmUtil.deriveKey(DEFAULT_KEY_PASSWORD);
        } else {
            this.aesKeyHex = AesGcmUtil.deriveKey(configuredKey);
        }
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

        // 2. 身份证唯一校验（解密比对）
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
        if (dto.getRelationWithHolder() != null) update.setRelationWithHolder(dto.getRelationWithHolder());
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
