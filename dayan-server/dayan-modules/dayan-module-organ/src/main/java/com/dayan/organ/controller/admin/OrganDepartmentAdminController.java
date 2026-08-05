package com.dayan.organ.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.organ.entity.OrganDepartment;
import com.dayan.organ.service.OrganDepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端部门管理接口。
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class OrganDepartmentAdminController {

    private final OrganDepartmentService deptService;

    @Operation(summary = "部门列表（按组织）")
    @SaCheckPermission("organ:dept:list")
    @GetMapping
    public R<List<OrganDepartment>> list(@RequestParam(required = false) String organCode) {
        return R.ok(deptService.listByOrgan(organCode));
    }

    @Operation(summary = "新增部门")
    @SaCheckPermission("organ:dept:create")
    @PostMapping
    public R<String> create(@RequestBody OrganDepartment dept) {
        return R.ok(deptService.create(dept));
    }

    @Operation(summary = "修改部门")
    @SaCheckPermission("organ:dept:update")
    @PutMapping("/{organCode}/{deptCode}")
    public R<Void> update(@PathVariable String organCode, @PathVariable String deptCode,
                          @RequestBody OrganDepartment dept) {
        deptService.update(organCode, deptCode, dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @SaCheckPermission("organ:dept:delete")
    @DeleteMapping("/{organCode}/{deptCode}")
    public R<Void> delete(@PathVariable String organCode, @PathVariable String deptCode) {
        deptService.delete(organCode, deptCode);
        return R.ok();
    }
}
