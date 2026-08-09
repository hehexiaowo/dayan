package com.dayan.goods.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsSojournCreateDTO;
import com.dayan.goods.dto.GoodsSojournQueryDTO;
import com.dayan.goods.dto.GoodsSojournUpdateDTO;
import com.dayan.goods.service.GoodsSojournService;
import com.dayan.goods.vo.GoodsSojournVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端旅居 SKU 接口。
 *
 * <p>路径前缀 {@code /goods/sojourn}。
 */
@Tag(name = "商品旅居配置")
@RestController
@RequestMapping("/goods/sojourn")
@RequiredArgsConstructor
public class GoodsSojournAdminController {

    private final GoodsSojournService goodsSojournService;

    @Operation(summary = "旅居 SKU 分页列表")
    @SaCheckPermission("goods:sojourn:list")
    @GetMapping("/page")
    public R<PageResult<GoodsSojournVO>> page(GoodsSojournQueryDTO query) {
        return R.ok(goodsSojournService.page(query));
    }

    @Operation(summary = "旅居 SKU 列表（按商品）")
    @SaCheckPermission("goods:sojourn:list")
    @GetMapping("/list")
    public R<List<GoodsSojournVO>> list(@RequestParam String goodsCode) {
        return R.ok(goodsSojournService.listByGoods(goodsCode));
    }

    @Operation(summary = "旅居 SKU 详情")
    @SaCheckPermission("goods:sojourn:query")
    @GetMapping("/{id}")
    public R<GoodsSojournVO> getDetail(@PathVariable Long id) {
        return R.ok(goodsSojournService.getDetail(id));
    }

    @Operation(summary = "新增旅居 SKU")
    @SaCheckPermission("goods:sojourn:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid GoodsSojournCreateDTO dto) {
        return R.ok(goodsSojournService.create(dto));
    }

    @Operation(summary = "修改旅居 SKU")
    @SaCheckPermission("goods:sojourn:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody GoodsSojournUpdateDTO dto) {
        goodsSojournService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除旅居 SKU")
    @SaCheckPermission("goods:sojourn:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goodsSojournService.delete(id);
        return R.ok();
    }
}
