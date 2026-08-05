package com.dayan.organ.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.organ.entity.OrganDepartment;
import com.dayan.organ.mapper.OrganDepartmentMapper;
import com.dayan.organ.mapper.OrganEmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 部门管理服务。
 *
 * <p>核心：ancestors 祖级链维护。新增/移动部门时，ancestors = 父部门(ancestors + parentCode)。
 * 格式：{@code "OR00001,DEPT001,DEPT002"}（逗号分隔，含组织根 + 所有祖先 deptCode）。
 */
@Service
@RequiredArgsConstructor
public class OrganDepartmentService {

    private final OrganDepartmentMapper deptMapper;
    private final OrganEmployeeMapper employeeMapper;

    /**
     * 查询组织的部门树（平铺列表，前端按 parentCode 构建树）。
     */
    public List<OrganDepartment> listByOrgan(String organCode) {
        LambdaQueryWrapper<OrganDepartment> wrapper = new LambdaQueryWrapper<OrganDepartment>()
                .orderByAsc(OrganDepartment::getSortOrder);
        // organCode 为空时不加过滤（超管可查看全部组织部门）
        if (organCode != null && !organCode.isEmpty()) {
            wrapper.eq(OrganDepartment::getOrganCode, organCode);
        }
        return deptMapper.selectList(wrapper)
                .stream()
                .sorted(Comparator.comparingInt(d -> d.getSortOrder() == null ? 0 : d.getSortOrder()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public String create(OrganDepartment dept) {
        // 校验同级 deptCode 唯一
        Long count = deptMapper.selectCount(new LambdaQueryWrapper<OrganDepartment>()
                .eq(OrganDepartment::getOrganCode, dept.getOrganCode())
                .eq(OrganDepartment::getDeptCode, dept.getDeptCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "部门编码已存在: " + dept.getDeptCode());
        }
        // 维护 ancestors
        if (dept.getParentCode() == null || dept.getParentCode().isEmpty()) {
            // 顶级部门：ancestors = 组织根
            dept.setAncestors(dept.getOrganCode());
        } else {
            OrganDepartment parent = selectByCode(dept.getOrganCode(), dept.getParentCode());
            dept.setAncestors(parent.getAncestors() + "," + parent.getDeptCode());
        }
        if (dept.getStatus() == null) dept.setStatus(1);
        if (dept.getSortOrder() == null) dept.setSortOrder(0);
        deptMapper.insert(dept);
        return dept.getDeptCode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String organCode, String deptCode, OrganDepartment dept) {
        OrganDepartment existing = selectByCode(organCode, deptCode);
        dept.setId(existing.getId());
        deptMapper.updateById(dept);
    }

    /**
     * 删除部门（校验子部门 + 员工）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String organCode, String deptCode) {
        // 校验有无子部门
        Long childCount = deptMapper.selectCount(new LambdaQueryWrapper<OrganDepartment>()
                .eq(OrganDepartment::getOrganCode, organCode)
                .eq(OrganDepartment::getParentCode, deptCode));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "存在子部门，无法删除");
        }
        // 校验有无员工
        Long empCount = employeeMapper.selectCount(new LambdaQueryWrapper<com.dayan.organ.entity.OrganEmployee>()
                .eq(com.dayan.organ.entity.OrganEmployee::getOrganCode, organCode)
                .eq(com.dayan.organ.entity.OrganEmployee::getDeptCode, deptCode));
        if (empCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "部门下存在员工，无法删除");
        }
        deptMapper.delete(new LambdaQueryWrapper<OrganDepartment>()
                .eq(OrganDepartment::getOrganCode, organCode)
                .eq(OrganDepartment::getDeptCode, deptCode));
    }

    private OrganDepartment selectByCode(String organCode, String deptCode) {
        OrganDepartment dept = deptMapper.selectOne(new LambdaQueryWrapper<OrganDepartment>()
                .eq(OrganDepartment::getOrganCode, organCode)
                .eq(OrganDepartment::getDeptCode, deptCode)
                .last("LIMIT 1"));
        if (dept == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在: " + deptCode);
        }
        return dept;
    }
}
