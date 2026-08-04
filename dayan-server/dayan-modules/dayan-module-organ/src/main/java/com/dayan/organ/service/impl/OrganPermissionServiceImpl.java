package com.dayan.organ.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.organ.dto.OrganPermissionCreateDTO;
import com.dayan.organ.dto.OrganPermissionQueryDTO;
import com.dayan.organ.dto.OrganPermissionUpdateDTO;
import com.dayan.organ.entity.OrganPermission;
import com.dayan.organ.mapper.OrganPermissionMapper;
import com.dayan.organ.service.OrganPermissionService;
import com.dayan.organ.vo.OrganPermissionTreeVO;
import com.dayan.organ.vo.OrganPermissionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * organ 域权限项管理服务实现。
 *
 * <p>权限编码（permissionCode）全局唯一，由前端按"模块:资源:动作"约定传入（如 organ:role:list）。
 * 权限树按 permissionType 分组（1菜单 2按钮 3接口 4数据），组内按 parentCode 构建父子层级。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganPermissionServiceImpl implements OrganPermissionService {

    /** 权限类型：1菜单 2按钮 3接口 4数据 */
    private static final int TYPE_MENU = 1;
    private static final int TYPE_BUTTON = 2;
    private static final int TYPE_API = 3;
    private static final int TYPE_DATA = 4;

    private final OrganPermissionMapper permissionMapper;

    @Override
    public PageResult<OrganPermissionVO> page(OrganPermissionQueryDTO query) {
        LambdaQueryWrapper<OrganPermission> wrapper = new LambdaQueryWrapper<OrganPermission>()
                .eq(query.getPermissionType() != null, OrganPermission::getPermissionType, query.getPermissionType())
                .like(query.getPermissionName() != null && !query.getPermissionName().isEmpty(),
                        OrganPermission::getPermissionName, query.getPermissionName())
                .eq(query.getStatus() != null, OrganPermission::getStatus, query.getStatus())
                .orderByAsc(OrganPermission::getPermissionType)
                .orderByAsc(OrganPermission::getSortOrder)
                .orderByAsc(OrganPermission::getId);

        Page<OrganPermission> page = new Page<>(
                query.getCurrent() == null ? 1L : query.getCurrent(),
                query.getSize() == null ? 10L : query.getSize());
        IPage<OrganPermission> result = permissionMapper.selectPage(page, wrapper);

        List<OrganPermissionVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(OrganPermissionCreateDTO dto) {
        // permissionCode 全局唯一校验
        Long exist = permissionMapper.selectCount(new LambdaQueryWrapper<OrganPermission>()
                .eq(OrganPermission::getPermissionCode, dto.getPermissionCode()));
        if (exist != null && exist > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "权限编码已存在：" + dto.getPermissionCode());
        }

        OrganPermission entity = new OrganPermission();
        entity.setPermissionCode(dto.getPermissionCode());
        entity.setPermissionName(dto.getPermissionName());
        entity.setPermissionType(dto.getPermissionType());
        entity.setParentCode(dto.getParentCode());
        entity.setPath(dto.getPath());
        entity.setMethod(dto.getMethod());
        entity.setIcon(dto.getIcon());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        permissionMapper.insert(entity);
        log.info("创建权限成功: permissionCode={}, type={}", dto.getPermissionCode(), dto.getPermissionType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String permissionCode, OrganPermissionUpdateDTO dto) {
        OrganPermission existing = requirePermission(permissionCode);
        OrganPermission update = new OrganPermission();
        update.setId(existing.getId());
        if (dto.getPermissionName() != null) update.setPermissionName(dto.getPermissionName());
        if (dto.getPermissionType() != null) update.setPermissionType(dto.getPermissionType());
        if (dto.getParentCode() != null) update.setParentCode(dto.getParentCode());
        if (dto.getPath() != null) update.setPath(dto.getPath());
        if (dto.getMethod() != null) update.setMethod(dto.getMethod());
        if (dto.getIcon() != null) update.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        permissionMapper.updateById(update);
        log.info("更新权限成功: permissionCode={}", permissionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String permissionCode) {
        requirePermission(permissionCode);
        permissionMapper.delete(new LambdaQueryWrapper<OrganPermission>()
                .eq(OrganPermission::getPermissionCode, permissionCode));
        log.info("删除权限成功: permissionCode={}", permissionCode);
    }

    @Override
    public List<OrganPermissionVO> listAll() {
        List<OrganPermission> list = permissionMapper.selectList(new LambdaQueryWrapper<OrganPermission>()
                .ne(OrganPermission::getStatus, 0)
                .orderByAsc(OrganPermission::getPermissionType)
                .orderByAsc(OrganPermission::getSortOrder)
                .orderByAsc(OrganPermission::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<OrganPermissionTreeVO> tree() {
        List<OrganPermissionVO> all = listAll();

        // 1. 按 permissionType 分组（保持 1菜单 2按钮 3接口 4数据 顺序）
        Map<Integer, List<OrganPermissionVO>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        OrganPermissionVO::getPermissionType,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<OrganPermissionTreeVO> roots = new ArrayList<>();
        for (Map.Entry<Integer, List<OrganPermissionVO>> entry : grouped.entrySet()) {
            Integer type = entry.getKey();
            // 2. 类型分组虚拟节点
            OrganPermissionTreeVO groupNode = new OrganPermissionTreeVO();
            groupNode.setCode("group:" + type);
            groupNode.setName(typeName(type));
            groupNode.setPermissionType(type);
            groupNode.setGroup(true);
            groupNode.setSortOrder(type);

            // 3. 组内按 parentCode 构建父子层级
            List<OrganPermissionTreeVO> children = buildChildren(entry.getValue());
            groupNode.setChildren(children);
            roots.add(groupNode);
        }
        return roots;
    }

    // ====== 内部方法 ======

    private OrganPermission requirePermission(String permissionCode) {
        OrganPermission permission = permissionMapper.selectOne(new LambdaQueryWrapper<OrganPermission>()
                .eq(OrganPermission::getPermissionCode, permissionCode)
                .last("LIMIT 1"));
        if (permission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权限不存在：" + permissionCode);
        }
        return permission;
    }

    /**
     * 在同一类型分组内，按 parentCode 构建父子层级树。
     * parentCode 为空/null 视为顶级节点。
     */
    private List<OrganPermissionTreeVO> buildChildren(List<OrganPermissionVO> permissions) {
        Map<String, OrganPermissionTreeVO> nodeMap = new LinkedHashMap<>();
        List<OrganPermissionTreeVO> nodes = permissions.stream()
                .sorted(Comparator
                        .comparingInt((OrganPermissionVO p) -> p.getSortOrder() == null ? 0 : p.getSortOrder())
                        .thenComparingLong(p -> p.getId() == null ? 0L : p.getId()))
                .map(p -> {
                    OrganPermissionTreeVO node = toTreeNode(p);
                    nodeMap.put(node.getPermissionCode(), node);
                    return node;
                })
                .collect(Collectors.toList());

        List<OrganPermissionTreeVO> roots = new ArrayList<>();
        for (OrganPermissionTreeVO node : nodes) {
            String parentCode = node.getParentCode();
            if (parentCode == null || parentCode.isEmpty() || !nodeMap.containsKey(parentCode)) {
                roots.add(node);
            } else {
                nodeMap.get(parentCode).getChildren().add(node);
            }
        }
        return roots;
    }

    private OrganPermissionTreeVO toTreeNode(OrganPermissionVO vo) {
        OrganPermissionTreeVO node = new OrganPermissionTreeVO();
        node.setCode(vo.getPermissionCode());
        node.setName(vo.getPermissionName());
        node.setPermissionType(vo.getPermissionType());
        node.setGroup(false);
        node.setPermissionCode(vo.getPermissionCode());
        node.setParentCode(vo.getParentCode());
        node.setIcon(vo.getIcon());
        node.setSortOrder(vo.getSortOrder());
        return node;
    }

    private String typeName(int type) {
        switch (type) {
            case TYPE_MENU: return "菜单";
            case TYPE_BUTTON: return "按钮";
            case TYPE_API: return "接口";
            case TYPE_DATA: return "数据";
            default: return "其他";
        }
    }

    private OrganPermissionVO toVO(OrganPermission entity) {
        OrganPermissionVO vo = new OrganPermissionVO();
        vo.setId(entity.getId());
        vo.setPermissionCode(entity.getPermissionCode());
        vo.setPermissionName(entity.getPermissionName());
        vo.setParentCode(entity.getParentCode());
        vo.setPermissionType(entity.getPermissionType());
        vo.setPath(entity.getPath());
        vo.setMethod(entity.getMethod());
        vo.setIcon(entity.getIcon());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
