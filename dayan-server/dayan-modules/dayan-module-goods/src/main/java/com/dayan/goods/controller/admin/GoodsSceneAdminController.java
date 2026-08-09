package com.dayan.goods.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsSceneCreateDTO;
import com.dayan.goods.dto.GoodsSceneQueryDTO;
import com.dayan.goods.dto.GoodsSceneUpdateDTO;
import com.dayan.goods.service.GoodsSceneService;
import com.dayan.goods.vo.GoodsSceneVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端场景 SKU 接口。
 *
 * <p>路径前缀 {@code /goods/scene}。
 */
@Tag(name = "商品场景配置")
@RestController
@RequestMapping("/goods/scene")
@RequiredArgsConstructor
public class GoodsSceneAdminController {

    private final GoodsSceneService goodsSceneService;

    @Operation(summary = "场景 SKU 分页列表")
    @SaCheckPermission("goods:scene:list")
    @GetMapping("/page")
    public R<PageResult<GoodsSceneVO>> page(GoodsSceneQueryDTO query) {
        return R.ok(goodsSceneService.page(query));
    }

    @Operation(summary = "场景 SKU 列表（按商品）")
    @SaCheckPermission("goods:scene:list")
    @GetMapping("/list")
    public R<List<GoodsSceneVO>> list(@RequestParam String goodsCode) {
        return R.ok(goodsSceneService.listByGoods(goodsCode));
    }

    @Operation(summary = "场景 SKU 详情")
    @SaCheckPermission("goods:scene:query")
    @GetMapping("/{id}")
    public R<GoodsSceneVO> getDetail(@PathVariable Long id) {
        return R.ok(goodsSceneService.getDetail(id));
    }

    @Operation(summary = "新增场景 SKU")
    @SaCheckPermission("goods:scene:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid GoodsSceneCreateDTO dto) {
        return R.ok(goodsSceneService.create(dto));
    }

    @Operation(summary = "修改场景 SKU")
    @SaCheckPermission("goods:scene:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody GoodsSceneUpdateDTO dto) {
        goodsSceneService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除场景 SKU")
    @SaCheckPermission("goods:scene:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goodsSceneService.delete(id);
        return R.ok();
    }
}
