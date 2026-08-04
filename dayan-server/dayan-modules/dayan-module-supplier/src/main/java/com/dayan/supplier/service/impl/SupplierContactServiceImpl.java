package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.supplier.dto.SupplierContactCreateDTO;
import com.dayan.supplier.dto.SupplierContactQueryDTO;
import com.dayan.supplier.dto.SupplierContactUpdateDTO;
import com.dayan.supplier.entity.SupplierContact;
import com.dayan.supplier.mapper.SupplierContactMapper;
import com.dayan.supplier.service.SupplierContactService;
import com.dayan.supplier.vo.SupplierContactVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 供应商联系人服务实现。
 *
 * <p>主联系人唯一：同 supplierCode 下 {@code isPrimary=1} 仅 1 个。
 * 设主联系人时先把同 supplierCode 下所有联系人 isPrimary 置 0（事务内），再设当前为 1。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierContactServiceImpl implements SupplierContactService {

    private final SupplierContactMapper contactMapper;

    @Override
    public PageResult<SupplierContactVO> page(SupplierContactQueryDTO query) {
        LambdaQueryWrapper<SupplierContact> wrapper = new LambdaQueryWrapper<SupplierContact>()
                .eq(query.getSupplierCode() != null && !query.getSupplierCode().isEmpty(),
                        SupplierContact::getSupplierCode, query.getSupplierCode())
                .like(query.getContactName() != null && !query.getContactName().isEmpty(),
                        SupplierContact::getContactName, query.getContactName())
                .eq(query.getContactType() != null, SupplierContact::getContactType, query.getContactType())
                .eq(query.getIsPrimary() != null, SupplierContact::getIsPrimary, query.getIsPrimary())
                .orderByDesc(SupplierContact::getIsPrimary)
                .orderByAsc(SupplierContact::getId);
        Page<SupplierContact> page = contactMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SupplierContactVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public SupplierContactVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SupplierContactCreateDTO dto) {
        String supplierCode = dto.getSupplierCode();
        if (supplierCode == null || supplierCode.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "供应商编码不能为空");
        }
        Integer isPrimary = dto.getIsPrimary() == null ? 0 : dto.getIsPrimary();
        // 若设为主联系人，先把同 supplierCode 下其他联系人 isPrimary 置 0
        if (isPrimary == 1) {
            clearOtherPrimary(supplierCode);
        }

        SupplierContact entity = new SupplierContact();
        entity.setSupplierCode(supplierCode);
        entity.setContactName(dto.getContactName());
        entity.setContactType(dto.getContactType());
        entity.setPosition(dto.getPosition());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setWechat(dto.getWechat());
        entity.setIsPrimary(isPrimary);
        entity.setRemark(dto.getRemark());
        contactMapper.insert(entity);
        log.info("创建供应商联系人成功: id={}, supplierCode={}, contactName={}",
                entity.getId(), supplierCode, dto.getContactName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SupplierContactUpdateDTO dto) {
        SupplierContact existing = requireById(id);
        SupplierContact update = new SupplierContact();
        update.setId(existing.getId());

        if (dto.getContactName() != null) update.setContactName(dto.getContactName());
        if (dto.getContactType() != null) update.setContactType(dto.getContactType());
        if (dto.getPosition() != null) update.setPosition(dto.getPosition());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getEmail() != null) update.setEmail(dto.getEmail());
        if (dto.getWechat() != null) update.setWechat(dto.getWechat());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        if (dto.getIsPrimary() != null) {
            // 设为主联系人时，先把同 supplierCode 下其他联系人 isPrimary 置 0
            if (dto.getIsPrimary() == 1) {
                clearOtherPrimary(existing.getSupplierCode());
            }
            update.setIsPrimary(dto.getIsPrimary());
        }
        contactMapper.updateById(update);
        log.info("更新供应商联系人成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        contactMapper.deleteById(id);
        log.info("删除供应商联系人成功: id={}", id);
    }

    // ====== 内部方法 ======

    private SupplierContact requireById(Long id) {
        SupplierContact entity = contactMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商联系人不存在: id=" + id);
        }
        return entity;
    }

    /** 把同 supplierCode 下所有联系人 isPrimary 置 0 */
    private void clearOtherPrimary(String supplierCode) {
        contactMapper.update(null, new LambdaUpdateWrapper<SupplierContact>()
                .eq(SupplierContact::getSupplierCode, supplierCode)
                .eq(SupplierContact::getIsPrimary, 1)
                .set(SupplierContact::getIsPrimary, 0));
    }

    private SupplierContactVO toVO(SupplierContact entity) {
        SupplierContactVO vo = new SupplierContactVO();
        vo.setId(entity.getId());
        vo.setSupplierCode(entity.getSupplierCode());
        vo.setContactName(entity.getContactName());
        vo.setContactType(entity.getContactType());
        vo.setPosition(entity.getPosition());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setWechat(entity.getWechat());
        vo.setIsPrimary(entity.getIsPrimary());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
