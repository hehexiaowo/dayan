package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkRoomPriceCreateDTO;
import com.dayan.park.dto.ParkRoomPriceQueryDTO;
import com.dayan.park.dto.ParkRoomPriceUpdateDTO;
import com.dayan.park.service.ParkRoomPriceService;
import com.dayan.park.vo.ParkRoomPriceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端房型价格接口。
 *
 * <p>路径前缀 {@code /park/room-price}。
 */
@Tag(name = "机构房型价格管理")
@RestController
@RequestMapping("/park/room-price")
@RequiredArgsConstructor
public class ParkRoomPriceAdminController {

    private final ParkRoomPriceService parkRoomPriceService;

    @Operation(summary = "房型价格分页列表")
    @GetMapping("/page")
    public R<PageResult<ParkRoomPriceVO>> page(ParkRoomPriceQueryDTO query) {
        return R.ok(parkRoomPriceService.page(query));
    }

    @Operation(summary = "房型价格列表（按机构+房型）")
    @GetMapping("/list")
    public R<List<ParkRoomPriceVO>> list(@RequestParam String parkCode,
                                         @RequestParam String roomTypeCode) {
        return R.ok(parkRoomPriceService.listByRoomType(parkCode, roomTypeCode));
    }

    @Operation(summary = "房型价格详情")
    @GetMapping("/{id}")
    public R<ParkRoomPriceVO> getDetail(@PathVariable Long id) {
        return R.ok(parkRoomPriceService.getDetail(id));
    }

    @Operation(summary = "新增房型价格")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkRoomPriceCreateDTO dto) {
        return R.ok(parkRoomPriceService.create(dto));
    }

    @Operation(summary = "修改房型价格")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkRoomPriceUpdateDTO dto) {
        parkRoomPriceService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除房型价格")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkRoomPriceService.delete(id);
        return R.ok();
    }
}
