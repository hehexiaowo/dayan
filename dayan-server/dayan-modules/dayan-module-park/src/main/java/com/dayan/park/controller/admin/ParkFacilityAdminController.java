package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkFacilityCreateDTO;
import com.dayan.park.dto.ParkFacilityQueryDTO;
import com.dayan.park.dto.ParkFacilityUpdateDTO;
import com.dayan.park.service.ParkFacilityService;
import com.dayan.park.vo.ParkFacilityVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构设施接口。
 *
 * <p>路径前缀 {@code /park/facility}。
 */
@Tag(name = "机构设施管理")
@RestController
@RequestMapping("/park/facility")
@RequiredArgsConstructor
public class ParkFacilityAdminController {

    private final ParkFacilityService parkFacilityService;

    @Operation(summary = "机构设施分页列表")
    @SaCheckPermission("park:facility:list")
    @GetMapping("/page")
    public R<PageResult<ParkFacilityVO>> page(ParkFacilityQueryDTO query) {
        return R.ok(parkFacilityService.page(query));
    }

    @Operation(summary = "机构设施列表（按机构）")
    @SaCheckPermission("park:facility:list")
    @GetMapping("/list")
    public R<List<ParkFacilityVO>> list(@RequestParam String parkCode) {
        return R.ok(parkFacilityService.listByPark(parkCode));
    }

    @Operation(summary = "机构设施详情")
    @SaCheckPermission("park:facility:query")
    @GetMapping("/{id}")
    public R<ParkFacilityVO> getDetail(@PathVariable Long id) {
        return R.ok(parkFacilityService.getDetail(id));
    }

    @Operation(summary = "新增机构设施")
    @SaCheckPermission("park:facility:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkFacilityCreateDTO dto) {
        return R.ok(parkFacilityService.create(dto));
    }

    @Operation(summary = "修改机构设施")
    @SaCheckPermission("park:facility:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkFacilityUpdateDTO dto) {
        parkFacilityService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构设施")
    @SaCheckPermission("park:facility:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkFacilityService.delete(id);
        return R.ok();
    }
}
