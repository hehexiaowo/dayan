package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkFoodPriceCreateDTO;
import com.dayan.park.dto.ParkFoodPriceQueryDTO;
import com.dayan.park.dto.ParkFoodPriceUpdateDTO;
import com.dayan.park.service.ParkFoodPriceService;
import com.dayan.park.vo.ParkFoodPriceVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端餐饮价格接口。
 *
 * <p>路径前缀 {@code /park/food-price}。
 */
@Tag(name = "机构餐饮价格管理")
@RestController
@RequestMapping("/park/food-price")
@RequiredArgsConstructor
public class ParkFoodPriceAdminController {

    private final ParkFoodPriceService parkFoodPriceService;

    @Operation(summary = "餐饮价格分页列表")
    @SaCheckPermission("park:food-price:list")
    @GetMapping("/page")
    public R<PageResult<ParkFoodPriceVO>> page(ParkFoodPriceQueryDTO query) {
        return R.ok(parkFoodPriceService.page(query));
    }

    @Operation(summary = "餐饮价格列表（按机构+餐饮类型）")
    @SaCheckPermission("park:food-price:list")
    @GetMapping("/list")
    public R<List<ParkFoodPriceVO>> list(@RequestParam String parkCode,
                                         @RequestParam String foodTypeCode) {
        return R.ok(parkFoodPriceService.listByFoodType(parkCode, foodTypeCode));
    }

    @Operation(summary = "餐饮价格详情")
    @SaCheckPermission("park:food-price:query")
    @GetMapping("/{id}")
    public R<ParkFoodPriceVO> getDetail(@PathVariable Long id) {
        return R.ok(parkFoodPriceService.getDetail(id));
    }

    @Operation(summary = "新增餐饮价格")
    @SaCheckPermission("park:food-price:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkFoodPriceCreateDTO dto) {
        return R.ok(parkFoodPriceService.create(dto));
    }

    @Operation(summary = "修改餐饮价格")
    @SaCheckPermission("park:food-price:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkFoodPriceUpdateDTO dto) {
        parkFoodPriceService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除餐饮价格")
    @SaCheckPermission("park:food-price:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkFoodPriceService.delete(id);
        return R.ok();
    }
}
