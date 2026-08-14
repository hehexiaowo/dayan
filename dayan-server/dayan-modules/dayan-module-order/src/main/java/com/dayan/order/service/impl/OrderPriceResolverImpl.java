package com.dayan.order.service.impl;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.goods.entity.GoodsInfo;
import com.dayan.goods.mapper.GoodsInfoMapper;
import com.dayan.goods.service.GoodsSojournService;
import com.dayan.order.dto.CreateOrderSceneDTO;
import com.dayan.order.dto.CreateOrderSojournDTO;
import com.dayan.order.service.OrderPriceResolver;
import com.dayan.order.support.BillingCycleMath;
import com.dayan.order.support.PriceCheckMode;
import com.dayan.park.service.ParkPricingService;
import com.dayan.park.vo.ParkPricingVO;
import com.dayan.scene.service.SceneItemPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPriceResolverImpl implements OrderPriceResolver {

    private final ParkPricingService parkPricingService;
    private final SceneItemPriceService sceneItemPriceService;
    private final GoodsSojournService goodsSojournService;
    private final GoodsInfoMapper goodsInfoMapper;

    @Value("${dayan.order.price-check:strict}")
    private String priceCheckConfig;

    private PriceCheckMode mode() {
        return PriceCheckMode.parse(priceCheckConfig);
    }

    @Override
    public SojournAuthority resolveSojourn(CreateOrderSojournDTO dto, int stayDays) {
        if (mode() == PriceCheckMode.OFF) {
            return new SojournAuthority(null, null, null, null);
        }
        BigDecimal room = feeOf(dto.getParkCode(), 1, "room_type", dto.getRoomTypeCode(),
                dto.getRoomFee(), "房间费", stayDays, true);
        BigDecimal care = feeOf(dto.getParkCode(), 2, "care_type", dto.getCareTypeCode(),
                dto.getCareFee(), "照护费", stayDays, false);
        BigDecimal food = feeOf(dto.getParkCode(), 3, "food_type", dto.getFoodTypeCode(),
                dto.getFoodFee(), "餐饮费", stayDays, false);
        BigDecimal deposit = null;
        ParkPricingVO dep = parkPricingService.getCurrentFee(dto.getParkCode(), 4, "park", dto.getParkCode());
        if (dep != null) {
            deposit = BillingCycleMath.toOrderAmount(
                    dep.getSalePrice() != null ? dep.getSalePrice() : dep.getOriginalPrice(),
                    dep.getBillingCycle(), stayDays);
            checkDeviation("押金", dto.getDepositAmount(), deposit);
        }
        return new SojournAuthority(room, care, food, deposit);
    }

    /** 单项费用：查当前价→折算→偏差校验；required=true 且无定价时 strict 拒单 */
    private BigDecimal feeOf(String parkCode, Integer chargeType, String refType, String refCode,
                             BigDecimal clientValue, String label, int stayDays, boolean required) {
        if (refCode == null || refCode.isEmpty()) {
            if (required && mode() == PriceCheckMode.STRICT) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, label + "缺少关联类型编码，无法服务端核价");
            }
            if (required) {
                log.warn("[订单核价] {} 缺少关联编码，warn 模式回退客户端值 {}", label, clientValue);
                return null;
            }
            checkDeviation(label, clientValue, BigDecimal.ZERO);
            return BigDecimal.ZERO.setScale(2);
        }
        ParkPricingVO pricing = parkPricingService.getCurrentFee(parkCode, chargeType, refType, refCode);
        if (pricing == null || (pricing.getSalePrice() == null && pricing.getOriginalPrice() == null)) {
            if (mode() == PriceCheckMode.STRICT) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "该机构未配置" + label + "当前价（parkCode=" + parkCode + ", " + refType + "=" + refCode + "），请先在价格配置中维护");
            }
            log.warn("[订单核价] {} 无权威定价，回退客户端值 {}", label, clientValue);
            return null;
        }
        BigDecimal cyclePrice = pricing.getSalePrice() != null ? pricing.getSalePrice() : pricing.getOriginalPrice();
        BigDecimal authority = BillingCycleMath.toOrderAmount(cyclePrice, pricing.getBillingCycle(), stayDays);
        checkDeviation(label, clientValue, authority);
        return authority;
    }

    private void checkDeviation(String label, BigDecimal clientValue, BigDecimal authorityValue) {
        if (authorityValue == null || mode() == PriceCheckMode.OFF) {
            return;
        }
        if (BillingCycleMath.isDeviated(clientValue, authorityValue)) {
            if (mode() == PriceCheckMode.STRICT) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        label + "与服务端权威价偏差超限：客户端=" + clientValue + "，权威=" + authorityValue
                                + "，请核对价格配置或联系运营调价");
            }
            log.warn("[订单核价] {} 偏差：客户端={}，权威={}（warn 模式按权威价入账）", label, clientValue, authorityValue);
        }
    }

    @Override
    public BigDecimal resolveSceneUnitPrice(CreateOrderSceneDTO dto) {
        if (mode() == PriceCheckMode.OFF) {
            return null;
        }
        BigDecimal authority = null;
        if (dto.getActivityDate() != null) {
            authority = sceneItemPriceService.getCurrentPersonPrice(dto.getSceneCode(), dto.getActivityDate());
        }
        if (authority == null && dto.getGoodsCode() != null && !dto.getGoodsCode().isEmpty()) {
            GoodsInfo goods = goodsInfoMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GoodsInfo>()
                    .eq(GoodsInfo::getGoodsCode, dto.getGoodsCode()).last("LIMIT 1"));
            if (goods != null && goods.getSalePrice() != null) {
                authority = goods.getSalePrice();
            }
        }
        if (authority == null) {
            if (mode() == PriceCheckMode.STRICT) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "场景无有效价格（sceneCode=" + dto.getSceneCode() + "），请先配置 scene_item_price 或商品售价");
            }
            log.warn("[订单核价] 场景无权威价，回退客户端单价 {}", dto.getUnitPrice());
            return null;
        }
        checkDeviation("场景单价", dto.getUnitPrice(), authority);
        return authority;
    }
}
