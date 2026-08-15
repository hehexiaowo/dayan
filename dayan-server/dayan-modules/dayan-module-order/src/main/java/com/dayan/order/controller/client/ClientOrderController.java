package com.dayan.order.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.order.entity.OrderSojourn;
import com.dayan.order.mapper.OrderSojournMapper;
import com.dayan.order.vo.ClientOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client 端「我的订单」接口（旅游短居订单）。
 *
 * <p>路径：{@code /client-api/orders}。仅查询当前登录客户的旅游短居订单（order_sojourn），
 * clientCode 取自登录态 {@code ContextHolder.getAccountCode()}（loginId=clientCode），防越权。
 *
 * <p>当前仅含旅游短居订单；权益订单无 client_code 关联，经「我的权益」入口单独管理。
 * order_sojourn 的 deleted 由 MyBatis-Plus @TableLogic 自动过滤。
 *
 * <p>状态码 → 文案以 {@link com.dayan.order.enums.OrderEvent} 注释 + state_machine_seed 为准：
 * 0待支付/1已支付/2部分发放/3已发放/4已完成/5已取消/6退款中/7已退款。
 */
@Tag(name = "Client 我的订单")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class ClientOrderController {

    private final OrderSojournMapper orderSojournMapper;

    @Operation(summary = "我的旅游短居订单（分页，按状态分组）")
    @GetMapping
    public R<PageResult<ClientOrderVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "ALL") String group) {
        String clientCode = ContextHolder.getAccountCode();
        if (!StringUtils.hasText(clientCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        LambdaQueryWrapper<OrderSojourn> wrapper = new LambdaQueryWrapper<OrderSojourn>()
                .eq(OrderSojourn::getClientCode, clientCode)
                .orderByDesc(OrderSojourn::getCreatedAt);
        applyGroup(wrapper, group);

        Page<OrderSojourn> page = orderSojournMapper.selectPage(new Page<>(current, size), wrapper);
        List<ClientOrderVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return R.ok(new PageResult<>(current, size, page.getTotal(), records));
    }

    /** 状态分组 → order_status 条件（PENDING=待支付；ACTIVE=已支付/部分发放/已发放；DONE=已完成） */
    private void applyGroup(LambdaQueryWrapper<OrderSojourn> wrapper, String group) {
        if (group == null) {
            return;
        }
        switch (group.toUpperCase()) {
            case "PENDING" -> wrapper.eq(OrderSojourn::getOrderStatus, 0);
            case "ACTIVE" -> wrapper.in(OrderSojourn::getOrderStatus, 1, 2, 3);
            case "DONE" -> wrapper.in(OrderSojourn::getOrderStatus, 4);
            case "ALL" -> { /* 无状态过滤 */ }
            default -> { /* 未知分组当 ALL */ }
        }
    }

    private ClientOrderVO toVO(OrderSojourn o) {
        return ClientOrderVO.builder()
                .orderCode(o.getOrderCode())
                .orderStatus(o.getOrderStatus())
                .statusText(statusText(o.getOrderStatus()))
                .title(o.getParkFullName())
                .parkName(o.getParkFullName())
                .skuName(o.getSkuName())
                .checkinDate(o.getCheckinDate())
                .checkoutDate(o.getCheckoutDate())
                .stayDays(o.getStayDays())
                .payAmount(o.getPayAmount())
                .totalAmount(o.getTotalAmount())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private static final Map<Integer, String> STATUS_TEXT = Map.of(
            0, "待支付", 1, "已支付", 2, "部分发放", 3, "已发放",
            4, "已完成", 5, "已取消", 6, "退款中", 7, "已退款");

    private String statusText(Integer status) {
        return status == null ? "" : STATUS_TEXT.getOrDefault(status, "");
    }
}
