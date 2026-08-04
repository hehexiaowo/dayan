package com.dayan.goods.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsSkuEquityCreateDTO;
import com.dayan.goods.dto.GoodsSkuEquityQueryDTO;
import com.dayan.goods.dto.GoodsSkuEquityUpdateDTO;
import com.dayan.goods.service.GoodsSkuEquityService;
import com.dayan.goods.vo.GoodsSkuEquityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权益 SKU 接口。
 *
 * <p>路径前缀 {@code /goods/sku-equity}。
 */
@Tag(name = "商品权益SKU管理")
@RestController
@RequestMapping("/goods/sku-equity")
@RequiredArgsConstructor
public class GoodsSkuEquityAdminController {

    private final GoodsSkuEquityService goodsSkuEquityService;

    @Operation(summary = "权益 SKU 分页列表")
    @GetMapping("/page")
    public R<PageResult<GoodsSkuEquityVO>> page(GoodsSkuEquityQueryDTO query) {
        return R.ok(goodsSkuEquityService.page(query));
    }

    @Operation(summary = "权益 SKU 列表（按商品）")
    @GetMapping("/list")
    public R<List<GoodsSkuEquityVO>> list(@RequestParam String goodsCode) {
        return R.ok(goodsSkuEquityService.listByGoods(goodsCode));
    }

    @Operation(summary = "权益 SKU 详情")
    @GetMapping("/{id}")
    public R<GoodsSkuEquityVO> getDetail(@PathVariable Long id) {
        return R.ok(goodsSkuEquityService.getDetail(id));
    }

    @Operation(summary = "新增权益 SKU")
    @PostMapping
    public R<Long> create(@RequestBody @Valid GoodsSkuEquityCreateDTO dto) {
        return R.ok(goodsSkuEquityService.create(dto));
    }

    @Operation(summary = "修改权益 SKU")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody GoodsSkuEquityUpdateDTO dto) {
        goodsSkuEquityService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除权益 SKU")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goodsSkuEquityService.delete(id);
        return R.ok();
    }
}
