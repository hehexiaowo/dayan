package com.dayan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkPricingCreateDTO;
import com.dayan.park.dto.ParkPricingQueryDTO;
import com.dayan.park.dto.ParkPricingReviseDTO;
import com.dayan.park.dto.ParkPricingUpdateDTO;
import com.dayan.park.service.ParkPricingService;
import com.dayan.park.vo.ParkPricingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构统一定价接口。
 *
 * <p>路径前缀 {@code /park/pricing}。
 * 合并原 room-price / care-price / food-price / facility-price / service-price 五个 Controller。
 */
@Tag(name = "机构定价管理")
@RestController
@RequestMapping("/park/pricing")
@RequiredArgsConstructor
public class ParkPricingAdminController {

    private final ParkPricingService parkPricingService;

    @Operation(summary = "定价分页列表")
    @SaCheckPermission("park:pricing:list")
    @GetMapping("/page")
    public R<PageResult<ParkPricingVO>> page(ParkPricingQueryDTO query) {
        return R.ok(parkPricingService.page(query));
    }

    @Operation(summary = "按关联类型+编码列表（展开行专用）")
    @SaCheckPermission("park:pricing:list")
    @GetMapping("/list")
    public R<List<ParkPricingVO>> listByRef(@RequestParam String parkCode,
                                             @RequestParam String refType,
                                             @RequestParam String refCode) {
        return R.ok(parkPricingService.listByRef(parkCode, refType, refCode));
    }

    @Operation(summary = "按费类列表（押金/房间/照护/餐费维度）")
    @SaCheckPermission("park:pricing:list")
    @GetMapping("/charge-type/{chargeType}")
    public R<List<ParkPricingVO>> listByChargeType(@RequestParam String parkCode,
                                                    @PathVariable Integer chargeType) {
        return R.ok(parkPricingService.listByChargeType(parkCode, chargeType));
    }

    @Operation(summary = "定价详情")
    @SaCheckPermission("park:pricing:query")
    @GetMapping("/{id}")
    public R<ParkPricingVO> getDetail(@PathVariable Long id) {
        return R.ok(parkPricingService.getDetail(id));
    }

    @Operation(summary = "新增定价")
    @SaCheckPermission("park:pricing:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkPricingCreateDTO dto) {
        return R.ok(parkPricingService.create(dto));
    }

    @Operation(summary = "修改定价")
    @SaCheckPermission("park:pricing:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkPricingUpdateDTO dto) {
        parkPricingService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "调价（版本化：立即/预约生效）")
    @SaCheckPermission("park:pricing:update")
    @PostMapping("/{id}/revise")
    public R<Long> revise(@PathVariable Long id, @RequestBody @Valid ParkPricingReviseDTO dto) {
        return R.ok(parkPricingService.revise(id, dto));
    }

    @Operation(summary = "删除定价")
    @SaCheckPermission("park:pricing:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkPricingService.delete(id);
        return R.ok();
    }
}
