package com.dayan.goods.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsDisplayBlockCreateDTO;
import com.dayan.goods.dto.GoodsDisplayBlockQueryDTO;
import com.dayan.goods.dto.GoodsDisplayBlockUpdateDTO;
import com.dayan.goods.service.GoodsDisplayBlockService;
import com.dayan.goods.vo.GoodsDisplayBlockVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端商品展示板块接口。
 *
 * <p>路径前缀 {@code /goods/display-block}。
 */
@Tag(name = "商品展示板块管理")
@RestController
@RequestMapping("/goods/display-block")
@RequiredArgsConstructor
public class GoodsDisplayBlockAdminController {

    private final GoodsDisplayBlockService goodsDisplayBlockService;

    @Operation(summary = "展示板块分页列表")
    @SaCheckPermission("goods:display-block:list")
    @GetMapping("/page")
    public R<PageResult<GoodsDisplayBlockVO>> page(GoodsDisplayBlockQueryDTO query) {
        return R.ok(goodsDisplayBlockService.page(query));
    }

    @Operation(summary = "展示板块列表（按商品）")
    @SaCheckPermission("goods:display-block:list")
    @GetMapping("/list")
    public R<List<GoodsDisplayBlockVO>> list(@RequestParam String goodsCode) {
        return R.ok(goodsDisplayBlockService.listByGoods(goodsCode));
    }

    @Operation(summary = "展示板块详情")
    @SaCheckPermission("goods:display-block:query")
    @GetMapping("/{id}")
    public R<GoodsDisplayBlockVO> getDetail(@PathVariable Long id) {
        return R.ok(goodsDisplayBlockService.getDetail(id));
    }

    @Operation(summary = "新增展示板块")
    @SaCheckPermission("goods:display-block:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid GoodsDisplayBlockCreateDTO dto) {
        return R.ok(goodsDisplayBlockService.create(dto));
    }

    @Operation(summary = "修改展示板块")
    @SaCheckPermission("goods:display-block:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody GoodsDisplayBlockUpdateDTO dto) {
        goodsDisplayBlockService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除展示板块")
    @SaCheckPermission("goods:display-block:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goodsDisplayBlockService.delete(id);
        return R.ok();
    }
}
