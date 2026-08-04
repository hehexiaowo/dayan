package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkServiceItemCreateDTO;
import com.dayan.park.dto.ParkServiceItemQueryDTO;
import com.dayan.park.dto.ParkServiceItemUpdateDTO;
import com.dayan.park.service.ParkServiceItemService;
import com.dayan.park.vo.ParkServiceItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构服务项接口。
 *
 * <p>路径前缀 {@code /park/service-item}。
 */
@Tag(name = "机构服务项管理")
@RestController
@RequestMapping("/park/service-item")
@RequiredArgsConstructor
public class ParkServiceItemAdminController {

    private final ParkServiceItemService parkServiceItemService;

    @Operation(summary = "机构服务项分页列表")
    @GetMapping("/page")
    public R<PageResult<ParkServiceItemVO>> page(ParkServiceItemQueryDTO query) {
        return R.ok(parkServiceItemService.page(query));
    }

    @Operation(summary = "机构服务项列表（按机构）")
    @GetMapping("/list")
    public R<List<ParkServiceItemVO>> list(@RequestParam String parkCode) {
        return R.ok(parkServiceItemService.listByPark(parkCode));
    }

    @Operation(summary = "机构服务项详情")
    @GetMapping("/{id}")
    public R<ParkServiceItemVO> getDetail(@PathVariable Long id) {
        return R.ok(parkServiceItemService.getDetail(id));
    }

    @Operation(summary = "新增机构服务项")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ParkServiceItemCreateDTO dto) {
        return R.ok(parkServiceItemService.create(dto));
    }

    @Operation(summary = "修改机构服务项")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ParkServiceItemUpdateDTO dto) {
        parkServiceItemService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构服务项")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        parkServiceItemService.delete(id);
        return R.ok();
    }
}
