package com.dayan.goods.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsSkuCourseCreateDTO;
import com.dayan.goods.dto.GoodsSkuCourseQueryDTO;
import com.dayan.goods.dto.GoodsSkuCourseUpdateDTO;
import com.dayan.goods.service.GoodsSkuCourseService;
import com.dayan.goods.vo.GoodsSkuCourseVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端课程 SKU 接口。
 *
 * <p>路径前缀 {@code /goods/sku-course}。
 */
@Tag(name = "商品课程SKU管理")
@RestController
@RequestMapping("/goods/sku-course")
@RequiredArgsConstructor
public class GoodsSkuCourseAdminController {

    private final GoodsSkuCourseService goodsSkuCourseService;

    @Operation(summary = "课程 SKU 分页列表")
    @SaCheckPermission("goods:sku-course:list")
    @GetMapping("/page")
    public R<PageResult<GoodsSkuCourseVO>> page(GoodsSkuCourseQueryDTO query) {
        return R.ok(goodsSkuCourseService.page(query));
    }

    @Operation(summary = "课程 SKU 列表（按商品）")
    @SaCheckPermission("goods:sku-course:list")
    @GetMapping("/list")
    public R<List<GoodsSkuCourseVO>> list(@RequestParam String goodsCode) {
        return R.ok(goodsSkuCourseService.listByGoods(goodsCode));
    }

    @Operation(summary = "课程 SKU 详情")
    @SaCheckPermission("goods:sku-course:query")
    @GetMapping("/{id}")
    public R<GoodsSkuCourseVO> getDetail(@PathVariable Long id) {
        return R.ok(goodsSkuCourseService.getDetail(id));
    }

    @Operation(summary = "新增课程 SKU")
    @SaCheckPermission("goods:sku-course:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid GoodsSkuCourseCreateDTO dto) {
        return R.ok(goodsSkuCourseService.create(dto));
    }

    @Operation(summary = "修改课程 SKU")
    @SaCheckPermission("goods:sku-course:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody GoodsSkuCourseUpdateDTO dto) {
        goodsSkuCourseService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除课程 SKU")
    @SaCheckPermission("goods:sku-course:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goodsSkuCourseService.delete(id);
        return R.ok();
    }
}
