package com.dayan.goods.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsSkuSojournCreateDTO;
import com.dayan.goods.dto.GoodsSkuSojournQueryDTO;
import com.dayan.goods.dto.GoodsSkuSojournUpdateDTO;
import com.dayan.goods.service.GoodsSkuSojournService;
import com.dayan.goods.vo.GoodsSkuSojournVO;
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
 * <p>路径前缀 {@code /goods/sku-sojourn}。
 */
@Tag(name = "商品旅居SKU管理")
@RestController
@RequestMapping("/goods/sku-sojourn")
@RequiredArgsConstructor
public class GoodsSkuSojournAdminController {

    private final GoodsSkuSojournService goodsSkuSojournService;

    @Operation(summary = "旅居 SKU 分页列表")
    @SaCheckPermission("goods:sku-sojourn:list")
    @GetMapping("/page")
    public R<PageResult<GoodsSkuSojournVO>> page(GoodsSkuSojournQueryDTO query) {
        return R.ok(goodsSkuSojournService.page(query));
    }

    @Operation(summary = "旅居 SKU 列表（按商品）")
    @SaCheckPermission("goods:sku-sojourn:list")
    @GetMapping("/list")
    public R<List<GoodsSkuSojournVO>> list(@RequestParam String goodsCode) {
        return R.ok(goodsSkuSojournService.listByGoods(goodsCode));
    }

    @Operation(summary = "旅居 SKU 详情")
    @SaCheckPermission("goods:sku-sojourn:query")
    @GetMapping("/{id}")
    public R<GoodsSkuSojournVO> getDetail(@PathVariable Long id) {
        return R.ok(goodsSkuSojournService.getDetail(id));
    }

    @Operation(summary = "新增旅居 SKU")
    @SaCheckPermission("goods:sku-sojourn:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid GoodsSkuSojournCreateDTO dto) {
        return R.ok(goodsSkuSojournService.create(dto));
    }

    @Operation(summary = "修改旅居 SKU")
    @SaCheckPermission("goods:sku-sojourn:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody GoodsSkuSojournUpdateDTO dto) {
        goodsSkuSojournService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除旅居 SKU")
    @SaCheckPermission("goods:sku-sojourn:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goodsSkuSojournService.delete(id);
        return R.ok();
    }
}
