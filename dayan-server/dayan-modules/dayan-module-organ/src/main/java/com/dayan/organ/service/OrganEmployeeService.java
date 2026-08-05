package com.dayan.organ.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.organ.entity.OrganEmployee;
import com.dayan.organ.mapper.OrganEmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 员工管理服务。
 */
@Service
@RequiredArgsConstructor
public class OrganEmployeeService {

    private final OrganEmployeeMapper employeeMapper;

    public PageResult<OrganEmployee> page(String organCode, String deptCode, String realName,
                                          long current, long size) {
        LambdaQueryWrapper<OrganEmployee> wrapper = new LambdaQueryWrapper<OrganEmployee>()
                .orderByDesc(OrganEmployee::getCreatedAt);
        // organCode 为空时不加过滤（超管可查看全部机构员工）
        if (organCode != null && !organCode.isEmpty()) {
            wrapper.eq(OrganEmployee::getOrganCode, organCode);
        }
        if (deptCode != null && !deptCode.isEmpty()) {
            wrapper.eq(OrganEmployee::getDeptCode, deptCode);
        }
        if (realName != null && !realName.isEmpty()) {
            wrapper.like(OrganEmployee::getRealName, realName);
        }
        Page<OrganEmployee> page = employeeMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    public List<OrganEmployee> listByDept(String organCode, String deptCode) {
        return employeeMapper.selectList(new LambdaQueryWrapper<OrganEmployee>()
                .eq(OrganEmployee::getOrganCode, organCode)
                .eq(OrganEmployee::getDeptCode, deptCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public String create(OrganEmployee employee) {
        // 工号唯一校验
        if (employee.getEmployeeCode() != null && !employee.getEmployeeCode().isEmpty()) {
            Long count = employeeMapper.selectCount(new LambdaQueryWrapper<OrganEmployee>()
                    .eq(OrganEmployee::getOrganCode, employee.getOrganCode())
                    .eq(OrganEmployee::getEmployeeCode, employee.getEmployeeCode()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "工号已存在: " + employee.getEmployeeCode());
            }
        }
        employeeMapper.insert(employee);
        return employee.getEmployeeCode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String organCode, String employeeCode, OrganEmployee employee) {
        OrganEmployee existing = employeeMapper.selectOne(new LambdaQueryWrapper<OrganEmployee>()
                .eq(OrganEmployee::getOrganCode, organCode)
                .eq(OrganEmployee::getEmployeeCode, employeeCode)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "员工不存在: " + employeeCode);
        }
        employee.setId(existing.getId());
        employeeMapper.updateById(employee);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String organCode, String employeeCode) {
        employeeMapper.delete(new LambdaQueryWrapper<OrganEmployee>()
                .eq(OrganEmployee::getOrganCode, organCode)
                .eq(OrganEmployee::getEmployeeCode, employeeCode));
    }
}
