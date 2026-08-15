package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.system.dto.SystemAssetCreateDTO;
import com.dayan.system.dto.SystemAssetQueryDTO;
import com.dayan.system.dto.SystemAssetUpdateDTO;
import com.dayan.system.service.SystemAssetService;
import com.dayan.system.vo.SystemAssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统素材库（system_asset）管理 Controller（admin 端）。
 *
 * 统一管理整个系统的文件与外链资源（本地 OSS 对象 / 外部存储链接）。
 * POST /register 供其他业务 tab 上传后幂等登记（复用 create 权限）。
 */
@Tag(name = "系统素材库管理")
@RestController
@RequestMapping("/system/asset")
@RequiredArgsConstructor
public class SystemAssetAdminController {

    private final SystemAssetService assetService;

    @Operation(summary = "素材分页查询")
    @SaCheckPermission("system:asset:list")
    @GetMapping("/page")
    public R<PageResult<SystemAssetVO>> page(SystemAssetQueryDTO query) {
        return R.ok(assetService.page(query));
    }

    @Operation(summary = "按机构编码查询全部素材")
    @SaCheckPermission("system:asset:list")
    @GetMapping("/list")
    public R<List<SystemAssetVO>> list(@RequestParam String parkCode,
                                     @RequestParam(value = "assetType", required = false) Integer assetType) {
        if (assetType != null) {
            return R.ok(assetService.listByParkAndType(parkCode, assetType));
        }
        return R.ok(assetService.listByPark(parkCode));
    }

    @Operation(summary = "素材详情")
    @SaCheckPermission("system:asset:query")
    @GetMapping("/{id}")
    public R<SystemAssetVO> getDetail(@PathVariable Long id) {
        return R.ok(assetService.getDetail(id));
    }

    @Operation(summary = "新增素材（本地OSS 或 外链）")
    @SaCheckPermission("system:asset:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SystemAssetCreateDTO dto) {
        return R.ok(assetService.create(dto));
    }

    @Operation(summary = "修改素材")
    @SaCheckPermission("system:asset:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SystemAssetUpdateDTO dto) {
        assetService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除素材")
    @SaCheckPermission("system:asset:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return R.ok();
    }

    @Operation(summary = "幂等登记素材（其他业务 tab 上传后自动登记）")
    @SaCheckPermission("system:asset:create")
    @PostMapping("/register")
    public R<Long> register(@RequestBody @Valid SystemAssetCreateDTO dto) {
        return R.ok(assetService.registerIfAbsent(
                dto.getParkCode(), dto.getAssetType(), dto.getAssetUrl(),
                dto.getSourceType(), dto.getSourceRefCode(),
                dto.getAssetName(), dto.getFileSize()));
    }
}
