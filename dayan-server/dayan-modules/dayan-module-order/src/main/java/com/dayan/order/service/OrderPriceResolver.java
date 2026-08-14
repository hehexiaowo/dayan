package com.dayan.order.service;

import com.dayan.order.dto.CreateOrderSceneDTO;
import com.dayan.order.dto.CreateOrderSojournDTO;

import java.math.BigDecimal;

/**
 * 订单服务端权威取价：替换「客户端传值直接入账」。
 *
 * 三态见 PriceCheckMode（dayan.order.price-check，默认 strict）。
 * strict：有权威项偏差、或 roomFee/unitPrice 无权威价 → 抛 PARAM_ERROR。
 * warn：偏差/缺失记日志；resolve* 返回权威金额：无权威定价的必选项在 strict 下已拒单；
 * 可选项（照护/餐饮）缺关联编码时权威为 0（该费用项按并入账，防止无依据收费）。
 */
public interface OrderPriceResolver {

    /** 旅居订单权威费用。返回 [roomFee, careFee, foodFee]；deposit 权威另查（null=透传客户端） */
    SojournAuthority resolveSojourn(CreateOrderSojournDTO dto, int stayDays);

    /** 场景订单权威单价（null=无权威，调用方按模式处理） */
    BigDecimal resolveSceneUnitPrice(CreateOrderSceneDTO dto);

    record SojournAuthority(BigDecimal roomFee, BigDecimal careFee, BigDecimal foodFee,
                            BigDecimal depositAmount) {}
}
