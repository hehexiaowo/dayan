package com.dayan.content.controller.client;

import com.dayan.common.core.enums.NetworkType;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.content.dto.ContentInfoQueryDTO;
import com.dayan.content.service.ContentInfoService;
import com.dayan.content.vo.ContentInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Client 端-内容查询 Controller（公开只读，口径同 ParkClientController）。
 *
 * 仅返回 contentStatus=2（已发布）+ auditStatus=2（审核通过）；
 * network 过滤：内容 network_tags 为空=全部业态。
 * context-path /client-api 使实际 URL 为 /client-api/contents/*。
 */
@Tag(name = "Client 端-内容查询")
@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
public class ClientContentController {

    private final ContentInfoService contentInfoService;

    @Operation(summary = "已发布内容分页（可按业态过滤）")
    @GetMapping
    public R<PageResult<ContentInfoVO>> page(@RequestParam(value = "network", required = false) String network,
                                             @RequestParam(required = false) String categoryCode,
                                             @RequestParam(required = false, defaultValue = "1") Long current,
                                             @RequestParam(required = false, defaultValue = "20") Long size) {
        ContentInfoQueryDTO query = publishedQuery(network);
        query.setCategoryCode(categoryCode);
        query.setCurrent(current);
        query.setSize(size);
        return R.ok(contentInfoService.page(query));
    }

    @Operation(summary = "内容详情（仅已发布；阅读量+1）")
    @GetMapping("/{contentCode}")
    public R<ContentInfoVO> detail(@PathVariable String contentCode) {
        ContentInfoVO vo = contentInfoService.getDetailByCode(contentCode);
        if (vo == null || !Integer.valueOf(2).equals(vo.getContentStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在或未发布: " + contentCode);
        }
        contentInfoService.incrementViewCount(contentCode);
        return R.ok(vo);
    }

    @Operation(summary = "首页轮播位（已发布+置顶/推荐，上限 8）")
    @GetMapping("/banners")
    public R<List<ContentInfoVO>> banners(@RequestParam(value = "network", required = false) String network) {
        return R.ok(topPublished(network, 8, true));
    }

    @Operation(summary = "推荐内容（已发布+推荐，上限由 limit 控制）")
    @GetMapping("/recommend")
    public R<List<ContentInfoVO>> recommend(@RequestParam(value = "network", required = false) String network,
                                            @RequestParam(required = false, defaultValue = "6") Integer limit) {
        return R.ok(topPublished(network, Math.min(limit, 20), false));
    }

    private ContentInfoQueryDTO publishedQuery(String network) {
        validateNetwork(network);
        ContentInfoQueryDTO query = new ContentInfoQueryDTO();
        query.setContentStatus(2);
        query.setAuditStatus(2);
        query.setNetwork(network);
        return query;
    }

    private List<ContentInfoVO> topPublished(String network, int limit, boolean bannerMode) {
        ContentInfoQueryDTO query = publishedQuery(network);
        query.setCurrent(1L);
        query.setSize((long) limit);
        if (bannerMode) {
            query.setIsTop(1);
        } else {
            query.setIsRecommend(1);
        }
        return contentInfoService.page(query).getRecords();
    }

    private void validateNetwork(String network) {
        if (network != null && !network.isBlank() && NetworkType.of(network) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法业态参数: " + network);
        }
    }
}
