package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.organ.entity.OrganAccountRoleRel;
import com.dayan.organ.entity.OrganPermission;
import com.dayan.organ.entity.OrganRoleMenuRel;
import com.dayan.organ.mapper.OrganAccountRoleRelMapper;
import com.dayan.organ.mapper.OrganPermissionMapper;
import com.dayan.organ.mapper.OrganRoleMenuRelMapper;
import com.dayan.system.entity.SystemMenu;
import com.dayan.system.mapper.SystemMenuMapper;
import com.dayan.system.vo.MenuGrantTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理服务。
 *
 * <p>菜单树形结构（parent_code），支持按 domain_type（admin/channel/agent/client）过滤四端可见性。
 *
 * <p>RBAC 数据权限：非超管账号仅可见其所属角色在 {@code organ_role_menu_rel} 中关联的菜单
 * （关联链：account_code → organ_account_role_rel.role_code → organ_role_menu_rel.menu_code → system_menu）。
 */
@Service
@RequiredArgsConstructor
public class SystemMenuService {

    private final SystemMenuMapper menuMapper;
    private final OrganAccountRoleRelMapper accountRoleRelMapper;
    private final OrganRoleMenuRelMapper roleMenuRelMapper;
    private final OrganPermissionMapper permissionMapper;

    /**
     * 查询全部菜单（按 sortOrder 排序），供前端构建树。
     *
     * @param domainType 端类型过滤（admin/channel/agent/client），null=全部
     */
    public List<SystemMenu> listAll(String domainType) {
        LambdaQueryWrapper<SystemMenu> wrapper = new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getStatus, 1)
                .orderByAsc(SystemMenu::getSortOrder);
        if (domainType != null && !domainType.isEmpty()) {
            wrapper.eq(SystemMenu::getDomainType, domainType);
        }
        return menuMapper.selectList(wrapper);
    }

    /**
     * 按当前登录账号查询可见菜单（平铺列表，RBAC 数据权限）。
     *
     * <p>数据权限链：
     * <ul>
     *   <li>超管（isAdmin=true）：返回全部菜单（按 domainType 过滤）</li>
     *   <li>非超管：account_code → organ_account_role_rel 取 roleCodes
     *       → organ_role_menu_rel 取 menuCodes → system_menu 过滤
     *       + <b>沿 parentCode 链向上回溯补全所有祖先目录</b>（确保 buildTree 能重建层级）</li>
     * </ul>
     *
     * @param domainType  端类型过滤（admin/channel/agent/client），null=全部
     * @param isAdmin     是否超管（true 走全量捷径）
     * @param accountCode 当前登录账号编码（非超管时必填，用于解析角色）
     */
    public List<SystemMenu> listByRole(String domainType, boolean isAdmin, String accountCode) {
        if (isAdmin) {
            return listAll(domainType);
        }
        List<SystemMenu> menus = listByRoleDirect(domainType, accountCode);
        return withAncestors(menus);
    }

    /**
     * 按当前登录账号查询可见菜单（组装树，RBAC 数据权限）。
     *
     * <p>与 {@link #listByRole} 数据源一致，区别是返回已组装 children 的树形结构，
     * 与 {@link #tree(String)} 契约对齐，供前端直接渲染侧边栏（无需自行 buildTree）。
     *
     * @param domainType  端类型过滤，null=全部
     * @param isAdmin     是否超管（true 走全量树捷径）
     * @param accountCode 当前登录账号编码（非超管时必填）
     */
    public List<SystemMenu> treeByRole(String domainType, boolean isAdmin, String accountCode) {
        if (isAdmin) {
            return tree(domainType);
        }
        List<SystemMenu> all = withAncestors(listByRoleDirect(domainType, accountCode));
        return buildTree(all, null);
    }

    /**
     * 非超管：account_code → roleCodes → menuCodes → system_menu（不含祖先补全）。
     */
    private List<SystemMenu> listByRoleDirect(String domainType, String accountCode) {
        // 1. 账号 → roleCodes
        List<String> roleCodes = accountRoleRelMapper.selectList(
                        new LambdaQueryWrapper<OrganAccountRoleRel>()
                                .eq(OrganAccountRoleRel::getAccountCode, accountCode))
                .stream()
                .map(OrganAccountRoleRel::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
        if (roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. roleCodes → menuCodes
        List<String> menuCodes = roleMenuRelMapper.selectList(
                        new LambdaQueryWrapper<OrganRoleMenuRel>()
                                .in(OrganRoleMenuRel::getRoleCode, roleCodes))
                .stream()
                .map(OrganRoleMenuRel::getMenuCode)
                .distinct()
                .collect(Collectors.toList());
        if (menuCodes.isEmpty()) {
            return Collections.emptyList();
        }
        // 3. menuCodes → system_menu（再叠加 status=1 + domainType 过滤）
        LambdaQueryWrapper<SystemMenu> wrapper = new LambdaQueryWrapper<SystemMenu>()
                .in(SystemMenu::getMenuCode, menuCodes)
                .eq(SystemMenu::getStatus, 1)
                .orderByAsc(SystemMenu::getSortOrder);
        if (domainType != null && !domainType.isEmpty()) {
            wrapper.eq(SystemMenu::getDomainType, domainType);
        }
        return menuMapper.selectList(wrapper);
    }

    /**
     * 沿 parentCode 链向上回溯，补全所有缺失的祖先目录。
     *
     * <p>非超管 RBAC 仅关联到具体菜单（menuType=2），但侧边栏树形渲染依赖父目录（menuType=1）存在。
     * 用迭代 + 单批 in() 查询补全，避免递归 N+1：
     * <ol>
     *   <li>从当前结果集收集所有 parentCode（结果集尚未包含的）</li>
     *   <li>单批查出这些缺失父级，并入结果集</li>
     *   <li>重复直到无新增缺失父级（parentCode=null 到达根）</li>
     * </ol>
     *
     * @param menus RBAC 直接命中的菜单（可能缺父目录）
     * @return 含所有祖先目录的完整菜单集合（去重，按 sortOrder 排序）
     */
    private List<SystemMenu> withAncestors(List<SystemMenu> menus) {
        if (menus.isEmpty()) {
            return menus;
        }
        Set<String> haveCodes = menus.stream()
                .map(SystemMenu::getMenuCode)
                .collect(Collectors.toCollection(HashSet::new));
        List<SystemMenu> result = new ArrayList<>(menus);
        // 迭代补全祖先，直到无新增缺失父级
        while (true) {
            List<String> missingParents = result.stream()
                    .map(SystemMenu::getParentCode)
                    .filter(p -> p != null && !p.isEmpty() && !haveCodes.contains(p))
                    .distinct()
                    .collect(Collectors.toList());
            if (missingParents.isEmpty()) {
                break;
            }
            List<SystemMenu> parents = menuMapper.selectList(
                    new LambdaQueryWrapper<SystemMenu>()
                            .in(SystemMenu::getMenuCode, missingParents)
                            .eq(SystemMenu::getStatus, 1));
            for (SystemMenu p : parents) {
                if (haveCodes.add(p.getMenuCode())) {
                    result.add(p);
                }
            }
        }
        result.sort(Comparator.comparingInt(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()));
        return result;
    }

    /**
     * 树形结构（嵌套 children）。
     */
    public List<SystemMenu> tree(String domainType) {
        List<SystemMenu> all = listAll(domainType);
        return buildTree(all, null);
    }

    private List<SystemMenu> buildTree(List<SystemMenu> all, String parentCode) {
        return all.stream()
                .filter(m -> (parentCode == null && m.getParentCode() == null)
                        || (parentCode != null && parentCode.equals(m.getParentCode())))
                .peek(m -> m.setChildren(buildTree(all, m.getMenuCode())))
                .sorted(Comparator.comparingInt(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .collect(Collectors.toList());
    }

    /** 操作权限类型：2按钮 3接口 */
    private static final List<Integer> GRANT_PERM_TYPES = java.util.Arrays.asList(2, 3);

    /**
     * 角色授权树（目录 → 菜单 → 操作权限）。
     *
     * <p>骨架为 admin 端启用菜单树；每个菜单节点 children 追加其归属操作权限
     * （organ_permission.menu_code 匹配、type∈(2,3)、status≠0）；末尾追加
     * 「其他权限」虚拟组挂 menu_code 为空的权限，保证任何权限都可被分配。
     */
    public List<MenuGrantTreeVO> grantTree() {
        List<SystemMenu> menus = listAll("admin");
        List<OrganPermission> perms = permissionMapper.selectList(
                new LambdaQueryWrapper<OrganPermission>()
                        .in(OrganPermission::getPermissionType, GRANT_PERM_TYPES)
                        .ne(OrganPermission::getStatus, 0)
                        .orderByAsc(OrganPermission::getSortOrder)
                        .orderByAsc(OrganPermission::getId));

        Map<String, List<OrganPermission>> permsByMenu = perms.stream()
                .filter(p -> p.getMenuCode() != null && !p.getMenuCode().isEmpty())
                .collect(Collectors.groupingBy(OrganPermission::getMenuCode));

        List<MenuGrantTreeVO> roots = buildGrantTree(menus, null, permsByMenu);

        // 孤儿判定：menu_code 为空，或指向不在骨架中的菜单（已停用/错配）——
        // 否则该权限在树上彻底消失、无法分配，违背「任何权限都可被分配」
        java.util.Set<String> renderedMenuCodes = menus.stream()
                .map(SystemMenu::getMenuCode).collect(Collectors.toSet());
        List<MenuGrantTreeVO> orphans = perms.stream()
                .filter(p -> p.getMenuCode() == null || p.getMenuCode().isEmpty()
                        || !renderedMenuCodes.contains(p.getMenuCode()))
                .map(this::toPermNode)
                .collect(Collectors.toList());
        if (!orphans.isEmpty()) {
            MenuGrantTreeVO group = new MenuGrantTreeVO();
            group.setNodeKey("group:other");
            group.setName("其他权限");
            group.setNodeType("GROUP");
            group.setChildren(orphans);
            roots.add(group);
        }
        return roots;
    }

    private List<MenuGrantTreeVO> buildGrantTree(List<SystemMenu> menus, String parentCode,
                                                 Map<String, List<OrganPermission>> permsByMenu) {
        return menus.stream()
                .filter(m -> (parentCode == null && m.getParentCode() == null)
                        || (parentCode != null && parentCode.equals(m.getParentCode())))
                .sorted(Comparator.comparingInt(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .map(m -> {
                    MenuGrantTreeVO node = new MenuGrantTreeVO();
                    node.setNodeKey("menu:" + m.getMenuCode());
                    node.setName(m.getMenuName());
                    node.setNodeType(m.getMenuType() != null && m.getMenuType() == 1 ? "DIR" : "MENU");
                    List<MenuGrantTreeVO> children = new ArrayList<>(
                            buildGrantTree(menus, m.getMenuCode(), permsByMenu));
                    for (OrganPermission p : permsByMenu.getOrDefault(m.getMenuCode(), Collections.emptyList())) {
                        children.add(toPermNode(p));
                    }
                    node.setChildren(children);
                    return node;
                })
                .collect(Collectors.toList());
    }

    private MenuGrantTreeVO toPermNode(OrganPermission p) {
        MenuGrantTreeVO node = new MenuGrantTreeVO();
        node.setNodeKey("perm:" + p.getPermissionCode());
        node.setName(p.getPermissionName());
        node.setNodeType("PERM");
        return node;
    }

    @Transactional(rollbackFor = Exception.class)
    public String create(SystemMenu menu) {
        if (menu.getParentCode() != null && menu.getParentCode().isEmpty()) {
            menu.setParentCode(null);
        }
        menuMapper.insert(menu);
        return menu.getMenuCode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String menuCode, SystemMenu menu) {
        SystemMenu existing = menuMapper.selectOne(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getMenuCode, menuCode).last("LIMIT 1"));
        if (existing == null) {
            throw new com.dayan.common.core.exception.NotFoundException("菜单不存在: " + menuCode);
        }
        menu.setId(existing.getId());
        menuMapper.updateById(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String menuCode) {
        menuMapper.delete(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getMenuCode, menuCode));
    }
}
