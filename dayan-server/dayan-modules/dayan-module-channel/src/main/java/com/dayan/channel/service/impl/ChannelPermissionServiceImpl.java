package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.channel.dto.ChannelPermissionCreateDTO;
import com.dayan.channel.dto.ChannelPermissionQueryDTO;
import com.dayan.channel.dto.ChannelPermissionUpdateDTO;
import com.dayan.channel.entity.ChannelPermission;
import com.dayan.channel.mapper.ChannelPermissionMapper;
import com.dayan.channel.service.ChannelPermissionService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道权限服务实现（P2 简化：基础 CRUD + 全量列表）。
 *
 * <p>{@code permissionCode} 全局唯一，由前端按"模块:资源:动作"约定传入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPermissionServiceImpl implements ChannelPermissionService {

    private final ChannelPermissionMapper permissionMapper;

    @Override
    public PageResult<ChannelPermission> page(ChannelPermissionQueryDTO query) {
        LambdaQueryWrapper<ChannelPermission> wrapper = new LambdaQueryWrapper<ChannelPermission>()
                .like(query.getPermissionName() != null && !query.getPermissionName().isEmpty(),
                        ChannelPermission::getPermissionName, query.getPermissionName())
                .eq(query.getPermissionType() != null,
                        ChannelPermission::getPermissionType, query.getPermissionType())
                .eq(query.getStatus() != null, ChannelPermission::getStatus, query.getStatus())
                .orderByAsc(ChannelPermission::getPermissionType)
                .orderByAsc(ChannelPermission::getSortOrder)
                .orderByAsc(ChannelPermission::getId);
        Page<ChannelPermission> page = permissionMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public ChannelPermission getDetail(String permissionCode) {
        return requirePermission(permissionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ChannelPermissionCreateDTO dto) {
        Long exist = permissionMapper.selectCount(new LambdaQueryWrapper<ChannelPermission>()
                .eq(ChannelPermission::getPermissionCode, dto.getPermissionCode()));
        if (exist != null && exist > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "权限编码已存在: " + dto.getPermissionCode());
        }
        ChannelPermission entity = new ChannelPermission();
        entity.setPermissionCode(dto.getPermissionCode());
        entity.setPermissionName(dto.getPermissionName());
        entity.setParentCode(dto.getParentCode());
        entity.setPermissionType(dto.getPermissionType());
        entity.setPath(dto.getPath());
        entity.setMethod(dto.getMethod());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        permissionMapper.insert(entity);
        log.info("创建渠道权限成功: permissionCode={}", dto.getPermissionCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String permissionCode, ChannelPermissionUpdateDTO dto) {
        ChannelPermission existing = requirePermission(permissionCode);
        ChannelPermission update = new ChannelPermission();
        update.setId(existing.getId());
        if (dto.getPermissionName() != null) update.setPermissionName(dto.getPermissionName());
        if (dto.getParentCode() != null) update.setParentCode(dto.getParentCode());
        if (dto.getPermissionType() != null) update.setPermissionType(dto.getPermissionType());
        if (dto.getPath() != null) update.setPath(dto.getPath());
        if (dto.getMethod() != null) update.setMethod(dto.getMethod());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        permissionMapper.updateById(update);
        log.info("更新渠道权限成功: permissionCode={}", permissionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String permissionCode) {
        requirePermission(permissionCode);
        permissionMapper.delete(new LambdaQueryWrapper<ChannelPermission>()
                .eq(ChannelPermission::getPermissionCode, permissionCode));
        log.info("删除渠道权限成功: permissionCode={}", permissionCode);
    }

    @Override
    public List<ChannelPermission> listAll() {
        return permissionMapper.selectList(new LambdaQueryWrapper<ChannelPermission>()
                .ne(ChannelPermission::getStatus, 0)
                .orderByAsc(ChannelPermission::getPermissionType)
                .orderByAsc(ChannelPermission::getSortOrder)
                .orderByAsc(ChannelPermission::getId));
    }

    @Override
    public List<ChannelPermission> tree() {
        List<ChannelPermission> all = listAll();
        java.util.Map<String, ChannelPermission> codeMap = new java.util.LinkedHashMap<>();
        for (ChannelPermission node : all) {
            codeMap.put(node.getPermissionCode(), node);
        }
        List<ChannelPermission> roots = new java.util.ArrayList<>();
        for (ChannelPermission node : all) {
            String parentCode = node.getParentCode();
            if (parentCode == null || parentCode.isEmpty() || !codeMap.containsKey(parentCode)) {
                roots.add(node);
            } else {
                ChannelPermission parent = codeMap.get(parentCode);
                if (parent.getChildren() == null) {
                    parent.setChildren(new java.util.ArrayList<>());
                }
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private ChannelPermission requirePermission(String permissionCode) {
        ChannelPermission permission = permissionMapper.selectOne(new LambdaQueryWrapper<ChannelPermission>()
                .eq(ChannelPermission::getPermissionCode, permissionCode)
                .last("LIMIT 1"));
        if (permission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "渠道权限不存在: " + permissionCode);
        }
        return permission;
    }
}
