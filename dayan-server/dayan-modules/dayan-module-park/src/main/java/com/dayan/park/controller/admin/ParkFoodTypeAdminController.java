package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkFoodTypeCreateDTO;
import com.dayan.park.dto.ParkFoodTypeQueryDTO;
import com.dayan.park.dto.ParkFoodTypeUpdateDTO;
import com.dayan.park.service.ParkFoodTypeService;
import com.dayan.park.vo.ParkFoodTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端餐饮类型接口。
 *
 * <p>路径前缀 {@code /park/food-type}。
 */
@Tag(name = "机构餐饮类型管理")
@RestController
@RequestMapping("/park/food-type")
@RequiredArgsConstructor
public class ParkFoodTypeAdminController {

    private final ParkFoodTypeService parkFoodTypeService;

    @Operation(summary = "餐饮类型分页列表")
    @GetMapping("/page")
    public R<PageResult<ParkFoodTypeVO>> page(ParkFoodTypeQueryDTO query) {
        return R.ok(parkFoodTypeService.page(query));
    }

    @Operation(summary = "餐饮类型列表（按机构）")
    @GetMapping("/list")
    public R<List<ParkFoodTypeVO>> list(@RequestParam String parkCode) {
        return R.ok(parkFoodTypeService.listByPark(parkCode));
    }

    @Operation(summary = "餐饮类型详情")
    @GetMapping("/{id}")
    public R<ParkFoodTypeVO> getDetail(@PathVariable Long id) {
        return R.ok(parkFoodTypeService.getDetail(id));
    }

    @Operation(summary = "新增餐饮类型")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkFoodTypeCreateDTO dto) {
        return R.ok(parkFoodTypeService.create(dto));
    }

    @Operation(summary = "修改餐饮类型")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkFoodTypeUpdateDTO dto) {
        parkFoodTypeService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除餐饮类型")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkFoodTypeService.delete(id);
        return R.ok();
    }
}
