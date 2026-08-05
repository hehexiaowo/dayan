package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkMediaVrCreateDTO;
import com.dayan.park.dto.ParkMediaVrQueryDTO;
import com.dayan.park.dto.ParkMediaVrUpdateDTO;
import com.dayan.park.service.ParkMediaVrService;
import com.dayan.park.vo.ParkMediaVrVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构 VR 接口。
 *
 * <p>路径前缀 {@code /park/media-vr}。
 */
@Tag(name = "机构VR管理")
@RestController
@RequestMapping("/park/media-vr")
@RequiredArgsConstructor
public class ParkMediaVrAdminController {

    private final ParkMediaVrService parkMediaVrService;

    @Operation(summary = "机构VR分页列表")
    @SaCheckPermission("park:media-vr:list")
    @GetMapping("/page")
    public R<PageResult<ParkMediaVrVO>> page(ParkMediaVrQueryDTO query) {
        return R.ok(parkMediaVrService.page(query));
    }

    @Operation(summary = "机构VR列表（按机构）")
    @SaCheckPermission("park:media-vr:list")
    @GetMapping("/list")
    public R<List<ParkMediaVrVO>> list(@RequestParam String parkCode) {
        return R.ok(parkMediaVrService.listByPark(parkCode));
    }

    @Operation(summary = "机构VR详情")
    @SaCheckPermission("park:media-vr:query")
    @GetMapping("/{id}")
    public R<ParkMediaVrVO> getDetail(@PathVariable Long id) {
        return R.ok(parkMediaVrService.getDetail(id));
    }

    @Operation(summary = "新增机构VR")
    @SaCheckPermission("park:media-vr:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkMediaVrCreateDTO dto) {
        return R.ok(parkMediaVrService.create(dto));
    }

    @Operation(summary = "修改机构VR")
    @SaCheckPermission("park:media-vr:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkMediaVrUpdateDTO dto) {
        parkMediaVrService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构VR")
    @SaCheckPermission("park:media-vr:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkMediaVrService.delete(id);
        return R.ok();
    }
}
