package com.dayan.goods.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.ServiceItemCreateDTO;
import com.dayan.goods.dto.ServiceItemQueryDTO;
import com.dayan.goods.dto.ServiceItemUpdateDTO;
import com.dayan.goods.service.ServiceItemService;
import com.dayan.goods.vo.ServiceItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端服务项目接口。
 *
 * <p>路径前缀 {@code /goods/service-item}（由 dayan-admin 启动模块 context-path=/admin-api 拼接为
 * {@code /admin-api/goods/service-item}）。
 */
@Tag(name = "服务项目管理")
@RestController
@RequestMapping("/goods/service-item")
@RequiredArgsConstructor
public class ServiceItemAdminController {

    private final ServiceItemService serviceItemService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @SaCheckPermission("goods:service-item:list")
    public R<PageResult<ServiceItemVO>> page(ServiceItemQueryDTO query) {
        return R.ok(serviceItemService.page(query));
    }

    @Operation(summary = "全量列表")
    @GetMapping("/list")
    @SaCheckPermission("goods:service-item:list")
    public R<List<ServiceItemVO>> list(ServiceItemQueryDTO query) {
        return R.ok(serviceItemService.list(query));
    }

    @Operation(summary = "详情")
    @GetMapping("/{itemCode}")
    @SaCheckPermission("goods:service-item:query")
    public R<ServiceItemVO> getDetail(@PathVariable String itemCode) {
        return R.ok(serviceItemService.getDetail(itemCode));
    }

    @Operation(summary = "创建")
    @PostMapping
    @SaCheckPermission("goods:service-item:create")
    public R<String> create(@Valid @RequestBody ServiceItemCreateDTO dto) {
        return R.ok(serviceItemService.create(dto));
    }

    @Operation(summary = "更新")
    @PutMapping("/{itemCode}")
    @SaCheckPermission("goods:service-item:update")
    public R<Void> update(@PathVariable String itemCode, @Valid @RequestBody ServiceItemUpdateDTO dto) {
        serviceItemService.update(itemCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{itemCode}")
    @SaCheckPermission("goods:service-item:delete")
    public R<Void> delete(@PathVariable String itemCode) {
        serviceItemService.delete(itemCode);
        return R.ok();
    }
}
