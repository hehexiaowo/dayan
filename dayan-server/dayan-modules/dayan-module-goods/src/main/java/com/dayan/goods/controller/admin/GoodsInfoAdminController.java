package com.dayan.goods.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsInfoCreateDTO;
import com.dayan.goods.dto.GoodsInfoQueryDTO;
import com.dayan.goods.dto.GoodsInfoShelfDTO;
import com.dayan.goods.dto.GoodsInfoUpdateDTO;
import com.dayan.goods.service.GoodsInfoService;
import com.dayan.goods.vo.GoodsInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端商品 SPU 接口。
 *
 * <p>路径前缀 {@code /goods/info}（由 dayan-admin 启动模块 context-path=/admin-api 拼接为
 * {@code /admin-api/goods/info}）。
 */
@Tag(name = "商品SPU管理")
@RestController
@RequestMapping("/goods/info")
@RequiredArgsConstructor
public class GoodsInfoAdminController {

    private final GoodsInfoService goodsInfoService;

    @Operation(summary = "商品分页列表")
    @GetMapping("/page")
    public R<PageResult<GoodsInfoVO>> page(GoodsInfoQueryDTO query) {
        return R.ok(goodsInfoService.page(query));
    }

    @Operation(summary = "商品列表（轻量）")
    @GetMapping("/list")
    public R<List<GoodsInfoVO>> list(GoodsInfoQueryDTO query) {
        return R.ok(goodsInfoService.list(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{goodsCode}")
    public R<GoodsInfoVO> getDetail(@PathVariable String goodsCode) {
        return R.ok(goodsInfoService.getDetail(goodsCode));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public R<String> create(@RequestBody @Valid GoodsInfoCreateDTO dto) {
        return R.ok(goodsInfoService.create(dto));
    }

    @Operation(summary = "修改商品")
    @PutMapping("/{goodsCode}")
    public R<Void> update(@PathVariable String goodsCode,
                          @RequestBody GoodsInfoUpdateDTO dto) {
        goodsInfoService.update(goodsCode, dto);
        return R.ok();
    }

    @Operation(summary = "商品上下架")
    @PostMapping("/shelf")
    public R<Void> shelf(@RequestBody @Valid GoodsInfoShelfDTO dto) {
        goodsInfoService.shelf(dto);
        return R.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{goodsCode}")
    public R<Void> delete(@PathVariable String goodsCode) {
        goodsInfoService.delete(goodsCode);
        return R.ok();
    }
}
