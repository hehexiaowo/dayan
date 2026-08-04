package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkRoomTypeCreateDTO;
import com.dayan.park.dto.ParkRoomTypeQueryDTO;
import com.dayan.park.dto.ParkRoomTypeUpdateDTO;
import com.dayan.park.service.ParkRoomTypeService;
import com.dayan.park.vo.ParkRoomTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端房型接口。
 *
 * <p>路径前缀 {@code /park/room-type}。
 */
@Tag(name = "机构房型管理")
@RestController
@RequestMapping("/park/room-type")
@RequiredArgsConstructor
public class ParkRoomTypeAdminController {

    private final ParkRoomTypeService parkRoomTypeService;

    @Operation(summary = "房型分页列表")
    @GetMapping("/page")
    public R<PageResult<ParkRoomTypeVO>> page(ParkRoomTypeQueryDTO query) {
        return R.ok(parkRoomTypeService.page(query));
    }

    @Operation(summary = "房型列表（按机构）")
    @GetMapping("/list")
    public R<List<ParkRoomTypeVO>> list(@RequestParam String parkCode) {
        return R.ok(parkRoomTypeService.listByPark(parkCode));
    }

    @Operation(summary = "房型详情")
    @GetMapping("/{id}")
    public R<ParkRoomTypeVO> getDetail(@PathVariable Long id) {
        return R.ok(parkRoomTypeService.getDetail(id));
    }

    @Operation(summary = "新增房型")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkRoomTypeCreateDTO dto) {
        return R.ok(parkRoomTypeService.create(dto));
    }

    @Operation(summary = "修改房型")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkRoomTypeUpdateDTO dto) {
        parkRoomTypeService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除房型")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkRoomTypeService.delete(id);
        return R.ok();
    }
}
