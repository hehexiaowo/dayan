package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkServiceTypeCreateDTO;
import com.dayan.park.dto.ParkServiceTypeQueryDTO;
import com.dayan.park.dto.ParkServiceTypeUpdateDTO;
import com.dayan.park.service.ParkServiceTypeService;
import com.dayan.park.vo.ParkServiceTypeVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构服务类型接口。
 *
 * <p>路径前缀 {@code /park/service-type}。
 */
@Tag(name = "机构服务类型管理")
@RestController
@RequestMapping("/park/service-type")
@RequiredArgsConstructor
public class ParkServiceTypeAdminController {

    private final ParkServiceTypeService parkServiceTypeService;

    @Operation(summary = "机构服务类型分页列表")
    @SaCheckPermission("park:service-type:list")
    @GetMapping("/page")
    public R<PageResult<ParkServiceTypeVO>> page(ParkServiceTypeQueryDTO query) {
        return R.ok(parkServiceTypeService.page(query));
    }

    @Operation(summary = "机构服务类型列表（按机构）")
    @SaCheckPermission("park:service-type:list")
    @GetMapping("/list")
    public R<List<ParkServiceTypeVO>> list(@RequestParam String parkCode) {
        return R.ok(parkServiceTypeService.listByPark(parkCode));
    }

    @Operation(summary = "机构服务类型详情")
    @SaCheckPermission("park:service-type:query")
    @GetMapping("/{id}")
    public R<ParkServiceTypeVO> getDetail(@PathVariable Long id) {
        return R.ok(parkServiceTypeService.getDetail(id));
    }

    @Operation(summary = "新增机构服务类型")
    @SaCheckPermission("park:service-type:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkServiceTypeCreateDTO dto) {
        return R.ok(parkServiceTypeService.create(dto));
    }

    @Operation(summary = "修改机构服务类型")
    @SaCheckPermission("park:service-type:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkServiceTypeUpdateDTO dto) {
        parkServiceTypeService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构服务类型")
    @SaCheckPermission("park:service-type:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkServiceTypeService.delete(id);
        return R.ok();
    }
}
