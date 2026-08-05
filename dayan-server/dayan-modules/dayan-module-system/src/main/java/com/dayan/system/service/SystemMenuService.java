package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.organ.entity.OrganAccountRoleRel;
import com.dayan.organ.entity.OrganRoleMenuRel;
import com.dayan.organ.mapper.OrganAccountRoleRelMapper;
import com.dayan.organ.mapper.OrganRoleMenuRelMapper;
import com.dayan.system.entity.SystemMenu;
import com.dayan.system.mapper.SystemMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
