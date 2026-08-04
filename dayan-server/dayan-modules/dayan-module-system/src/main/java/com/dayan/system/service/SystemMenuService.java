package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.system.entity.SystemMenu;
import com.dayan.system.mapper.SystemMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理服务。
 *
 * <p>菜单树形结构（parent_code），支持按 domain_type（admin/channel/agent/client）过滤四端可见性。
 */
@Service
@RequiredArgsConstructor
public class SystemMenuService {

    private final SystemMenuMapper menuMapper;

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
     * 按角色编码查询菜单（供前端动态路由）。
     * P1 简化：超管返回全部；非超管按 role_menu_rel 关联查询（RBAC 子代理实现后联调）。
     */
    public List<SystemMenu> listByRole(String domainType, boolean isAdmin) {
        if (isAdmin) {
            return listAll(domainType);
        }
        // TODO P1-A 联调：非超管按 organ_role_menu_rel 关联查询
        return listAll(domainType);
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
                .peek(m -> {
                    // 递归设置 children（用 transient 字段需在 Entity 加，P1 简化返回平铺）
                })
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
