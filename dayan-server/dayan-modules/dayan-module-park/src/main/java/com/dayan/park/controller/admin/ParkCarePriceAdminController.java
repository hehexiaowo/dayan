package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkCarePriceCreateDTO;
import com.dayan.park.dto.ParkCarePriceQueryDTO;
import com.dayan.park.dto.ParkCarePriceUpdateDTO;
import com.dayan.park.service.ParkCarePriceService;
import com.dayan.park.vo.ParkCarePriceVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端照护价格接口。
 *
 * <p>路径前缀 {@code /park/care-price}。
 */
@Tag(name = "机构照护价格管理")
@RestController
@RequestMapping("/park/care-price")
@RequiredArgsConstructor
public class ParkCarePriceAdminController {

    private final ParkCarePriceService parkCarePriceService;

    @Operation(summary = "照护价格分页列表")
    @SaCheckPermission("park:care-price:list")
    @GetMapping("/page")
    public R<PageResult<ParkCarePriceVO>> page(ParkCarePriceQueryDTO query) {
        return R.ok(parkCarePriceService.page(query));
    }

    @Operation(summary = "照护价格列表（按机构+照护类型）")
    @SaCheckPermission("park:care-price:list")
    @GetMapping("/list")
    public R<List<ParkCarePriceVO>> list(@RequestParam String parkCode,
                                         @RequestParam String careTypeCode) {
        return R.ok(parkCarePriceService.listByCareType(parkCode, careTypeCode));
    }

    @Operation(summary = "照护价格详情")
    @SaCheckPermission("park:care-price:query")
    @GetMapping("/{id}")
    public R<ParkCarePriceVO> getDetail(@PathVariable Long id) {
        return R.ok(parkCarePriceService.getDetail(id));
    }

    @Operation(summary = "新增照护价格")
    @SaCheckPermission("park:care-price:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkCarePriceCreateDTO dto) {
        return R.ok(parkCarePriceService.create(dto));
    }

    @Operation(summary = "修改照护价格")
    @SaCheckPermission("park:care-price:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkCarePriceUpdateDTO dto) {
        parkCarePriceService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除照护价格")
    @SaCheckPermission("park:care-price:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkCarePriceService.delete(id);
        return R.ok();
    }
}
