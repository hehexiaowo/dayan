package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkFacilityTypeCreateDTO;
import com.dayan.park.dto.ParkFacilityTypeQueryDTO;
import com.dayan.park.dto.ParkFacilityTypeUpdateDTO;
import com.dayan.park.service.ParkFacilityTypeService;
import com.dayan.park.vo.ParkFacilityTypeVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构设施类型接口。
 *
 * <p>路径前缀 {@code /park/facility-type}。
 */
@Tag(name = "机构设施类型管理")
@RestController
@RequestMapping("/park/facility-type")
@RequiredArgsConstructor
public class ParkFacilityTypeAdminController {

    private final ParkFacilityTypeService parkFacilityTypeService;

    @Operation(summary = "机构设施类型分页列表")
    @SaCheckPermission("park:facility-type:list")
    @GetMapping("/page")
    public R<PageResult<ParkFacilityTypeVO>> page(ParkFacilityTypeQueryDTO query) {
        return R.ok(parkFacilityTypeService.page(query));
    }

    @Operation(summary = "机构设施类型列表（按机构）")
    @SaCheckPermission("park:facility-type:list")
    @GetMapping("/list")
    public R<List<ParkFacilityTypeVO>> list(@RequestParam String parkCode) {
        return R.ok(parkFacilityTypeService.listByPark(parkCode));
    }

    @Operation(summary = "机构设施类型详情")
    @SaCheckPermission("park:facility-type:query")
    @GetMapping("/{id}")
    public R<ParkFacilityTypeVO> getDetail(@PathVariable Long id) {
        return R.ok(parkFacilityTypeService.getDetail(id));
    }

    @Operation(summary = "新增机构设施类型")
    @SaCheckPermission("park:facility-type:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkFacilityTypeCreateDTO dto) {
        return R.ok(parkFacilityTypeService.create(dto));
    }

    @Operation(summary = "修改机构设施类型")
    @SaCheckPermission("park:facility-type:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkFacilityTypeUpdateDTO dto) {
        parkFacilityTypeService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构设施类型")
    @SaCheckPermission("park:facility-type:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkFacilityTypeService.delete(id);
        return R.ok();
    }
}
