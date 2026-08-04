package com.dayan.organ.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.organ.entity.OrganEmployee;
import com.dayan.organ.service.OrganEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端员工管理接口。
 */
@Tag(name = "员工管理")
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class OrganEmployeeAdminController {

    private final OrganEmployeeService employeeService;

    @Operation(summary = "员工分页列表")
    @SaCheckPermission("organ:employee:list")
    @GetMapping
    public R<PageResult<OrganEmployee>> page(
            @RequestParam String organCode,
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) String realName,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(employeeService.page(organCode, deptCode, realName, current, size));
    }

    @Operation(summary = "按部门查询员工")
    @SaCheckPermission("organ:employee:list")
    @GetMapping("/by-dept/{organCode}/{deptCode}")
    public R<java.util.List<OrganEmployee>> listByDept(@PathVariable String organCode,
                                                        @PathVariable String deptCode) {
        return R.ok(employeeService.listByDept(organCode, deptCode));
    }

    @Operation(summary = "新增员工")
    @SaCheckPermission("organ:employee:create")
    @PostMapping
    public R<String> create(@RequestBody OrganEmployee employee) {
        return R.ok(employeeService.create(employee));
    }

    @Operation(summary = "修改员工")
    @SaCheckPermission("organ:employee:update")
    @PutMapping("/{organCode}/{employeeCode}")
    public R<Void> update(@PathVariable String organCode, @PathVariable String employeeCode,
                          @RequestBody OrganEmployee employee) {
        employeeService.update(organCode, employeeCode, employee);
        return R.ok();
    }

    @Operation(summary = "删除员工")
    @SaCheckPermission("organ:employee:delete")
    @DeleteMapping("/{organCode}/{employeeCode}")
    public R<Void> delete(@PathVariable String organCode, @PathVariable String employeeCode) {
        employeeService.delete(organCode, employeeCode);
        return R.ok();
    }
}
