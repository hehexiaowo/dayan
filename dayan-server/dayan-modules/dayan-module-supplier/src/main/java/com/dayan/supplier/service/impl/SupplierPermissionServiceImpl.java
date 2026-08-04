package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.supplier.dto.SupplierPermissionCreateDTO;
import com.dayan.supplier.dto.SupplierPermissionQueryDTO;
import com.dayan.supplier.dto.SupplierPermissionUpdateDTO;
import com.dayan.supplier.entity.SupplierPermission;
import com.dayan.supplier.mapper.SupplierPermissionMapper;
import com.dayan.supplier.service.SupplierPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 供应商权限服务实现（P3 简化：基础 CRUD + 全量列表）。
 *
 * <p>{@code permissionCode} 全局唯一，由前端按"模块:资源:动作"约定传入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPermissionServiceImpl implements SupplierPermissionService {

    private final SupplierPermissionMapper permissionMapper;

    @Override
    public PageResult<SupplierPermission> page(SupplierPermissionQueryDTO query) {
        LambdaQueryWrapper<SupplierPermission> wrapper = new LambdaQueryWrapper<SupplierPermission>()
                .like(query.getPermissionName() != null && !query.getPermissionName().isEmpty(),
                        SupplierPermission::getPermissionName, query.getPermissionName())
                .eq(query.getPermissionType() != null,
                        SupplierPermission::getPermissionType, query.getPermissionType())
                .eq(query.getStatus() != null, SupplierPermission::getStatus, query.getStatus())
                .orderByAsc(SupplierPermission::getPermissionType)
                .orderByAsc(SupplierPermission::getSortOrder)
                .orderByAsc(SupplierPermission::getId);
        Page<SupplierPermission> page = permissionMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public SupplierPermission getDetail(String permissionCode) {
        return requirePermission(permissionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SupplierPermissionCreateDTO dto) {
        Long exist = permissionMapper.selectCount(new LambdaQueryWrapper<SupplierPermission>()
                .eq(SupplierPermission::getPermissionCode, dto.getPermissionCode()));
        if (exist != null && exist > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "权限编码已存在: " + dto.getPermissionCode());
        }
        SupplierPermission entity = new SupplierPermission();
        entity.setPermissionCode(dto.getPermissionCode());
        entity.setPermissionName(dto.getPermissionName());
        entity.setParentCode(dto.getParentCode());
        entity.setPermissionType(dto.getPermissionType());
        entity.setPath(dto.getPath());
        entity.setMethod(dto.getMethod());
        entity.setIcon(dto.getIcon());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        permissionMapper.insert(entity);
        log.info("创建供应商权限成功: permissionCode={}", dto.getPermissionCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String permissionCode, SupplierPermissionUpdateDTO dto) {
        SupplierPermission existing = requirePermission(permissionCode);
        SupplierPermission update = new SupplierPermission();
        update.setId(existing.getId());
        if (dto.getPermissionName() != null) update.setPermissionName(dto.getPermissionName());
        if (dto.getParentCode() != null) update.setParentCode(dto.getParentCode());
        if (dto.getPermissionType() != null) update.setPermissionType(dto.getPermissionType());
        if (dto.getPath() != null) update.setPath(dto.getPath());
        if (dto.getMethod() != null) update.setMethod(dto.getMethod());
        if (dto.getIcon() != null) update.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        permissionMapper.updateById(update);
        log.info("更新供应商权限成功: permissionCode={}", permissionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String permissionCode) {
        requirePermission(permissionCode);
        permissionMapper.delete(new LambdaQueryWrapper<SupplierPermission>()
                .eq(SupplierPermission::getPermissionCode, permissionCode));
        log.info("删除供应商权限成功: permissionCode={}", permissionCode);
    }

    @Override
    public List<SupplierPermission> listAll() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SupplierPermission>()
                .ne(SupplierPermission::getStatus, 0)
                .orderByAsc(SupplierPermission::getPermissionType)
                .orderByAsc(SupplierPermission::getSortOrder)
                .orderByAsc(SupplierPermission::getId));
    }

    private SupplierPermission requirePermission(String permissionCode) {
        SupplierPermission permission = permissionMapper.selectOne(new LambdaQueryWrapper<SupplierPermission>()
                .eq(SupplierPermission::getPermissionCode, permissionCode)
                .last("LIMIT 1"));
        if (permission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商权限不存在: " + permissionCode);
        }
        return permission;
    }
}
