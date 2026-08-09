package com.dayan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkAssetCreateDTO;
import com.dayan.park.dto.ParkAssetQueryDTO;
import com.dayan.park.dto.ParkAssetUpdateDTO;
import com.dayan.park.service.ParkAssetService;
import com.dayan.park.vo.ParkAssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机构素材库（park_asset）管理 Controller（admin 端）。
 *
 * 统一管理所有来源的图片/视频/文件/VR。
 * POST /register 供其他业务 tab 上传后幂等注册（复用 create 权限）。
 */
@Tag(name = "机构素材库管理")
@RestController
@RequestMapping("/park/asset")
@RequiredArgsConstructor
public class ParkAssetAdminController {

    private final ParkAssetService assetService;

    @Operation(summary = "素材分页查询")
    @SaCheckPermission("park:asset:list")
    @GetMapping("/page")
    public R<PageResult<ParkAssetVO>> page(ParkAssetQueryDTO query) {
        return R.ok(assetService.page(query));
    }

    @Operation(summary = "按机构编码查询全部素材")
    @SaCheckPermission("park:asset:list")
    @GetMapping("/list")
    public R<List<ParkAssetVO>> list(@RequestParam String parkCode,
                                     @RequestParam(value = "assetType", required = false) Integer assetType) {
        if (assetType != null) {
            return R.ok(assetService.listByParkAndType(parkCode, assetType));
        }
        return R.ok(assetService.listByPark(parkCode));
    }

    @Operation(summary = "素材详情")
    @SaCheckPermission("park:asset:query")
    @GetMapping("/{id}")
    public R<ParkAssetVO> getDetail(@PathVariable Long id) {
        return R.ok(assetService.getDetail(id));
    }

    @Operation(summary = "新增素材")
    @SaCheckPermission("park:asset:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkAssetCreateDTO dto) {
        return R.ok(assetService.create(dto));
    }

    @Operation(summary = "修改素材")
    @SaCheckPermission("park:asset:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody ParkAssetUpdateDTO dto) {
        assetService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除素材")
    @SaCheckPermission("park:asset:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return R.ok();
    }

    @Operation(summary = "幂等注册素材（其他业务 tab 上传后自动注册）")
    @SaCheckPermission("park:asset:create")
    @PostMapping("/register")
    public R<Long> register(@RequestBody @Valid ParkAssetCreateDTO dto) {
        return R.ok(assetService.registerIfAbsent(
                dto.getParkCode(), dto.getAssetType(), dto.getAssetUrl(),
                dto.getSourceType(), dto.getSourceRefCode(),
                dto.getAssetName(), dto.getFileSize()));
    }
}
