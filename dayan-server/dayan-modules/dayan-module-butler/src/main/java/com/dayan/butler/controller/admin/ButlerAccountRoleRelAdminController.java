package com.dayan.butler.controller.admin;

import com.dayan.butler.dto.ButlerAccountRoleRelCreateDTO;
import com.dayan.butler.dto.ButlerAccountRoleRelQueryDTO;
import com.dayan.butler.dto.ButlerAccountRoleRelUpdateDTO;
import com.dayan.butler.service.ButlerAccountRoleRelService;
import com.dayan.butler.vo.ButlerAccountRoleRelVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端管家账号-角色关联接口（P5 仅 CRUD 框架，RBAC 查询后置）。
 *
 * <p>路径：{@code /butler/account-role-rel/*}。
 */
@Tag(name = "管家账号-角色关联管理")
@RestController
@RequestMapping("/butler/account-role-rel")
@RequiredArgsConstructor
public class ButlerAccountRoleRelAdminController {

    private final ButlerAccountRoleRelService butlerAccountRoleRelService;

    @Operation(summary = "管家账号-角色关联分页列表")
    @GetMapping("/page")
    public R<PageResult<ButlerAccountRoleRelVO>> page(ButlerAccountRoleRelQueryDTO query) {
        return R.ok(butlerAccountRoleRelService.page(query));
    }

    @Operation(summary = "管家账号-角色关联列表")
    @GetMapping("/list")
    public R<List<ButlerAccountRoleRelVO>> list(ButlerAccountRoleRelQueryDTO query) {
        return R.ok(butlerAccountRoleRelService.list(query));
    }

    @Operation(summary = "管家账号-角色关联详情")
    @GetMapping("/{id}")
    public R<ButlerAccountRoleRelVO> getDetail(@PathVariable Long id) {
        return R.ok(butlerAccountRoleRelService.getDetail(id));
    }

    @Operation(summary = "新增管家账号-角色关联")
    @OperationLog(module = "管家账号-角色关联", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ButlerAccountRoleRelCreateDTO dto) {
        return R.ok(butlerAccountRoleRelService.create(dto));
    }

    @Operation(summary = "修改管家账号-角色关联")
    @OperationLog(module = "管家账号-角色关联", action = "修改")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ButlerAccountRoleRelUpdateDTO dto) {
        butlerAccountRoleRelService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除管家账号-角色关联")
    @OperationLog(module = "管家账号-角色关联", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        butlerAccountRoleRelService.delete(id);
        return R.ok();
    }
}
