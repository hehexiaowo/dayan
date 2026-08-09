package com.dayan.goods.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsCourseCreateDTO;
import com.dayan.goods.dto.GoodsCourseQueryDTO;
import com.dayan.goods.dto.GoodsCourseUpdateDTO;
import com.dayan.goods.service.GoodsCourseService;
import com.dayan.goods.vo.GoodsCourseVO;
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
 * <p>路径前缀 {@code /goods/course}。
 */
@Tag(name = "商品课程配置")
@RestController
@RequestMapping("/goods/course")
@RequiredArgsConstructor
public class GoodsCourseAdminController {

    private final GoodsCourseService goodsCourseService;

    @Operation(summary = "课程 SKU 分页列表")
    @SaCheckPermission("goods:course:list")
    @GetMapping("/page")
    public R<PageResult<GoodsCourseVO>> page(GoodsCourseQueryDTO query) {
        return R.ok(goodsCourseService.page(query));
    }

    @Operation(summary = "课程 SKU 列表（按商品）")
    @SaCheckPermission("goods:course:list")
    @GetMapping("/list")
    public R<List<GoodsCourseVO>> list(@RequestParam String goodsCode) {
        return R.ok(goodsCourseService.listByGoods(goodsCode));
    }

    @Operation(summary = "课程 SKU 详情")
    @SaCheckPermission("goods:course:query")
    @GetMapping("/{id}")
    public R<GoodsCourseVO> getDetail(@PathVariable Long id) {
        return R.ok(goodsCourseService.getDetail(id));
    }

    @Operation(summary = "新增课程 SKU")
    @SaCheckPermission("goods:course:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid GoodsCourseCreateDTO dto) {
        return R.ok(goodsCourseService.create(dto));
    }

    @Operation(summary = "修改课程 SKU")
    @SaCheckPermission("goods:course:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody GoodsCourseUpdateDTO dto) {
        goodsCourseService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除课程 SKU")
    @SaCheckPermission("goods:course:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goodsCourseService.delete(id);
        return R.ok();
    }
}
