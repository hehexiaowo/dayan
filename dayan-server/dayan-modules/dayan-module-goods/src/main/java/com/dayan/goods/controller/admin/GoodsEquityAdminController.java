package com.dayan.goods.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.goods.dto.GoodsEquitySaveDTO;
import com.dayan.goods.service.GoodsEquityService;
import com.dayan.goods.vo.GoodsEquityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "权益商品配置")
@RestController
@RequestMapping("/goods/equity-config")
@RequiredArgsConstructor
public class GoodsEquityAdminController {

    private final GoodsEquityService goodsEquityService;

    @Operation(summary = "获取商品权益配置")
    @SaCheckPermission("goods:info:query")
    @GetMapping("/{goodsCode}")
    public R<GoodsEquityVO> get(@PathVariable String goodsCode) {
        return R.ok(goodsEquityService.getByGoodsCode(goodsCode));
    }

    @Operation(summary = "保存权益配置（新建或更新）")
    @SaCheckPermission("goods:info:update")
    @PostMapping
    public R<Void> save(@Valid @RequestBody GoodsEquitySaveDTO dto) {
        goodsEquityService.save(dto);
        return R.ok();
    }

    @Operation(summary = "删除权益配置")
    @SaCheckPermission("goods:info:update")
    @DeleteMapping("/{goodsCode}")
    public R<Void> delete(@PathVariable String goodsCode) {
        goodsEquityService.delete(goodsCode);
        return R.ok();
    }
}
