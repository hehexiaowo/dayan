package com.dayan.equity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.crypto.AesGcmUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.equity.dto.EquityChangeHolderQueryDTO;
import com.dayan.equity.entity.EquityChangeHolder;
import com.dayan.equity.mapper.EquityChangeHolderMapper;
import com.dayan.equity.service.EquityChangeHolderService;
import com.dayan.equity.vo.EquityChangeHolderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权益更换权益人记录服务实现（仅查询）。
 *
 * <p>身份证字段加密存储，查询时解密回传（管理端可见）。
 */
@Slf4j
@Service
public class EquityChangeHolderServiceImpl implements EquityChangeHolderService {

    private static final String DEFAULT_KEY_PASSWORD = "dayan-default-key";

    private final EquityChangeHolderMapper changeHolderMapper;
    private final String aesKeyHex;

    public EquityChangeHolderServiceImpl(
            EquityChangeHolderMapper changeHolderMapper,
            @Value("${dayan.aes.key:}") String configuredKey) {
        this.changeHolderMapper = changeHolderMapper;
        if (configuredKey == null || configuredKey.isBlank()) {
            this.aesKeyHex = AesGcmUtil.deriveKey(DEFAULT_KEY_PASSWORD);
        } else {
            this.aesKeyHex = AesGcmUtil.deriveKey(configuredKey);
        }
    }

    @Override
    public PageResult<EquityChangeHolderVO> page(EquityChangeHolderQueryDTO query) {
        LambdaQueryWrapper<EquityChangeHolder> wrapper = buildQueryWrapper(query);
        Page<EquityChangeHolder> page = changeHolderMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<EquityChangeHolderVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<EquityChangeHolderVO> listByEquity(String equityCode) {
        return changeHolderMapper.selectList(new LambdaQueryWrapper<EquityChangeHolder>()
                .eq(EquityChangeHolder::getEquityCode, equityCode)
                .orderByDesc(EquityChangeHolder::getOperateTime))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public EquityChangeHolderVO getDetail(Long id) {
        EquityChangeHolder entity = changeHolderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "更换记录不存在: id=" + id);
        }
        return toVO(entity);
    }

    private LambdaQueryWrapper<EquityChangeHolder> buildQueryWrapper(EquityChangeHolderQueryDTO query) {
        LambdaQueryWrapper<EquityChangeHolder> wrapper = new LambdaQueryWrapper<EquityChangeHolder>()
                .orderByDesc(EquityChangeHolder::getOperateTime);
        if (query.getEquityCode() != null && !query.getEquityCode().isEmpty()) {
            wrapper.eq(EquityChangeHolder::getEquityCode, query.getEquityCode());
        }
        if (query.getChangeStatus() != null) {
            wrapper.eq(EquityChangeHolder::getChangeStatus, query.getChangeStatus());
        }
        if (query.getOperatorCode() != null && !query.getOperatorCode().isEmpty()) {
            wrapper.eq(EquityChangeHolder::getOperatorCode, query.getOperatorCode());
        }
        return wrapper;
    }

    private EquityChangeHolderVO toVO(EquityChangeHolder entity) {
        EquityChangeHolderVO vo = new EquityChangeHolderVO();
        vo.setId(entity.getId());
        vo.setEquityCode(entity.getEquityCode());
        vo.setOldUsePersonCode(entity.getOldUsePersonCode());
        vo.setOldPersonName(entity.getOldPersonName());
        vo.setOldPersonIdCard(decryptSafely(entity.getOldPersonIdCard()));
        vo.setNewUsePersonCode(entity.getNewUsePersonCode());
        vo.setNewPersonName(entity.getNewPersonName());
        vo.setNewPersonIdCard(decryptSafely(entity.getNewPersonIdCard()));
        vo.setChangeReason(entity.getChangeReason());
        vo.setChangeStatus(entity.getChangeStatus());
        vo.setOperateTime(entity.getOperateTime());
        vo.setOperatorCode(entity.getOperatorCode());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
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
}
