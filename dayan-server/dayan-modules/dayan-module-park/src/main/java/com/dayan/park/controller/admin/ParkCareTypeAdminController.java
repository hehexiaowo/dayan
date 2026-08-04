package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkCareTypeCreateDTO;
import com.dayan.park.dto.ParkCareTypeQueryDTO;
import com.dayan.park.dto.ParkCareTypeUpdateDTO;
import com.dayan.park.service.ParkCareTypeService;
import com.dayan.park.vo.ParkCareTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端照护类型接口。
 *
 * <p>路径前缀 {@code /park/care-type}。
 */
@Tag(name = "机构照护类型管理")
@RestController
@RequestMapping("/park/care-type")
@RequiredArgsConstructor
public class ParkCareTypeAdminController {

    private final ParkCareTypeService parkCareTypeService;

    @Operation(summary = "照护类型分页列表")
    @GetMapping("/page")
    public R<PageResult<ParkCareTypeVO>> page(ParkCareTypeQueryDTO query) {
        return R.ok(parkCareTypeService.page(query));
    }

    @Operation(summary = "照护类型列表（按机构）")
    @GetMapping("/list")
    public R<List<ParkCareTypeVO>> list(@RequestParam String parkCode) {
        return R.ok(parkCareTypeService.listByPark(parkCode));
    }

    @Operation(summary = "照护类型详情")
    @GetMapping("/{id}")
    public R<ParkCareTypeVO> getDetail(@PathVariable Long id) {
        return R.ok(parkCareTypeService.getDetail(id));
    }

    @Operation(summary = "新增照护类型")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkCareTypeCreateDTO dto) {
        return R.ok(parkCareTypeService.create(dto));
    }

    @Operation(summary = "修改照护类型")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkCareTypeUpdateDTO dto) {
        parkCareTypeService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除照护类型")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkCareTypeService.delete(id);
        return R.ok();
    }
}
