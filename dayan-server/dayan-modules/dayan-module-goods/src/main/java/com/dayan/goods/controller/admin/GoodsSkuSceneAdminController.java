package com.dayan.goods.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsSkuSceneCreateDTO;
import com.dayan.goods.dto.GoodsSkuSceneQueryDTO;
import com.dayan.goods.dto.GoodsSkuSceneUpdateDTO;
import com.dayan.goods.service.GoodsSkuSceneService;
import com.dayan.goods.vo.GoodsSkuSceneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端场景 SKU 接口。
 *
 * <p>路径前缀 {@code /goods/sku-scene}。
 */
@Tag(name = "商品场景SKU管理")
@RestController
@RequestMapping("/goods/sku-scene")
@RequiredArgsConstructor
public class GoodsSkuSceneAdminController {

    private final GoodsSkuSceneService goodsSkuSceneService;

    @Operation(summary = "场景 SKU 分页列表")
    @GetMapping("/page")
    public R<PageResult<GoodsSkuSceneVO>> page(GoodsSkuSceneQueryDTO query) {
        return R.ok(goodsSkuSceneService.page(query));
    }

    @Operation(summary = "场景 SKU 列表（按商品）")
    @GetMapping("/list")
    public R<List<GoodsSkuSceneVO>> list(@RequestParam String goodsCode) {
        return R.ok(goodsSkuSceneService.listByGoods(goodsCode));
    }

    @Operation(summary = "场景 SKU 详情")
    @GetMapping("/{id}")
    public R<GoodsSkuSceneVO> getDetail(@PathVariable Long id) {
        return R.ok(goodsSkuSceneService.getDetail(id));
    }

    @Operation(summary = "新增场景 SKU")
    @PostMapping
    public R<Long> create(@RequestBody @Valid GoodsSkuSceneCreateDTO dto) {
        return R.ok(goodsSkuSceneService.create(dto));
    }

    @Operation(summary = "修改场景 SKU")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody GoodsSkuSceneUpdateDTO dto) {
        goodsSkuSceneService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除场景 SKU")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goodsSkuSceneService.delete(id);
        return R.ok();
    }
}
