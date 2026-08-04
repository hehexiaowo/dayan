package com.dayan.organ.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.organ.dto.OrganInfoCreateDTO;
import com.dayan.organ.dto.OrganInfoQueryDTO;
import com.dayan.organ.dto.OrganInfoUpdateDTO;
import com.dayan.organ.service.OrganInfoService;
import com.dayan.organ.vo.OrganInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端组织信息接口。
 */
@Tag(name = "组织管理")
@RestController
@RequestMapping("/organs")
@RequiredArgsConstructor
public class OrganInfoAdminController {

    private final OrganInfoService organInfoService;

    @Operation(summary = "组织分页列表")
    @SaCheckPermission("organ:info:list")
    @GetMapping
    public R<PageResult<OrganInfoVO>> page(OrganInfoQueryDTO query) {
        return R.ok(organInfoService.page(query));
    }

    @Operation(summary = "组织详情")
    @SaCheckPermission("organ:info:query")
    @GetMapping("/{organCode}")
    public R<OrganInfoVO> getDetail(@PathVariable String organCode) {
        return R.ok(organInfoService.getDetail(organCode));
    }

    @Operation(summary = "新增组织")
    @SaCheckPermission("organ:info:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid OrganInfoCreateDTO dto) {
        return R.ok(organInfoService.create(dto));
    }

    @Operation(summary = "修改组织")
    @SaCheckPermission("organ:info:update")
    @PutMapping("/{organCode}")
    public R<Void> update(@PathVariable String organCode, @RequestBody OrganInfoUpdateDTO dto) {
        organInfoService.update(organCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除组织")
    @SaCheckPermission("organ:info:delete")
    @DeleteMapping("/{organCode}")
    public R<Void> delete(@PathVariable String organCode) {
        organInfoService.delete(organCode);
        return R.ok();
    }
}
