package com.dayan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkScoreUpdateDTO;
import com.dayan.park.service.ParkScoreService;
import com.dayan.park.vo.ParkScoreVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端机构评分接口。
 *
 * <p>路径前缀 {@code /park/score}。
 */
@Tag(name = "机构评分管理")
@RestController
@RequestMapping("/park/score")
@RequiredArgsConstructor
public class ParkScoreAdminController {

    private final ParkScoreService parkScoreService;

    @Operation(summary = "获取机构评分")
    @SaCheckPermission("park:score:query")
    @GetMapping("/{parkCode}")
    public R<ParkScoreVO> getByParkCode(@PathVariable String parkCode) {
        return R.ok(parkScoreService.getByParkCode(parkCode));
    }

    @Operation(summary = "更新机构评分（upsert）")
    @SaCheckPermission("park:score:update")
    @PutMapping("/{parkCode}")
    public R<Void> upsert(@PathVariable String parkCode,
                          @RequestBody ParkScoreUpdateDTO dto) {
        parkScoreService.upsert(parkCode, dto);
        return R.ok();
    }
}
