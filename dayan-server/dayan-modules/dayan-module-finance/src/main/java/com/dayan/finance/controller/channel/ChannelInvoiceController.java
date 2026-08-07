package com.dayan.finance.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.finance.dto.ApplyInvoiceDTO;
import com.dayan.finance.dto.FinanceInvoiceQueryDTO;
import com.dayan.finance.service.FinanceInvoiceService;
import com.dayan.finance.vo.FinanceInvoiceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端发票申请接口。
 *
 * <p>路径：{@code /channel-api/finance-invoices/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>读接口（列表/详情）：finance_invoice 表无 channel_code 字段，靠
 * {@code applicantCode}(=channelCode) + {@code applicantType}("channel") 做归属过滤。
 */
@Tag(name = "Channel 发票申请")
@RestController
@RequestMapping("/finance-invoices")
@RequiredArgsConstructor
public class ChannelInvoiceController {

    private final FinanceInvoiceService financeInvoiceService;

    @Operation(summary = "渠道申请发票")
    @PostMapping("/apply")
    public R<String> apply(@RequestBody @Valid ApplyInvoiceDTO dto) {
        // 强制注入申请方信息，防止越权；applicantName 为快照字段，取当前登录账号姓名
        // （渠道登录时由 ChannelAuthServiceImpl 写入 Session = account.realName）
        dto.setApplicantCode(ContextHolder.getChannelCode());
        dto.setApplicantType("channel");
        dto.setApplicantName(ContextHolder.getAccountName());
        return R.ok(financeInvoiceService.apply(dto));
    }

    @Operation(summary = "本渠道发票列表")
    @SaCheckPermission("channel:invoice:list")
    @GetMapping
    public R<PageResult<FinanceInvoiceVO>> page(FinanceInvoiceQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        // 强制注入申请方信息（防越权），覆盖任何前端传入
        query.setApplicantCode(channelCode);
        query.setApplicantType("channel");
        return R.ok(financeInvoiceService.page(query));
    }

    @Operation(summary = "发票详情")
    @SaCheckPermission("channel:invoice:query")
    @GetMapping("/{invoiceCode}")
    public R<FinanceInvoiceVO> getDetail(@PathVariable String invoiceCode) {
        FinanceInvoiceVO vo = financeInvoiceService.getDetail(invoiceCode);
        String channelCode = ContextHolder.getChannelCode();
        if (vo == null
                || !channelCode.equals(vo.getApplicantCode())
                || !"channel".equals(vo.getApplicantType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发票不存在或无权访问");
        }
        return R.ok(vo);
    }
}
