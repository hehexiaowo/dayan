package com.dayan.equity.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.resp.R;
import com.dayan.common.security.StpKit;
import com.dayan.equity.dto.ActivateDTO;
import com.dayan.equity.dto.ClientActivateDTO;
import com.dayan.equity.dto.EquityDepotQueryDTO;
import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.enums.EquityEvent;
import com.dayan.equity.mapper.EquityDepotMapper;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.equity.service.EquityUsePersonService;
import com.dayan.equity.vo.EquityDepotVO;
import com.dayan.equity.vo.EquityUsePersonVO;
import com.dayan.goods.service.GoodsEquityService;
import com.dayan.goods.vo.GoodsEquityVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ClientServiceRequestDTO;
import com.dayan.service.dto.ServiceSessionCreateDTO;
import com.dayan.service.service.ServiceSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.dayan.service.util.QuotaYears;

/**
 * 权益域 - client 端接口（持卡人视角）。
 *
 * <p>路径：{@code /client-api/equity/*}。身份从 {@code StpKit.CLIENT} Session 获取 clientCode，
 * 所有查询强制按 clientCode 过滤，防越权。
 */
@Tag(name = "Client 权益")
@RestController
@RequestMapping("/equity")
@RequiredArgsConstructor
public class ClientEquityController {

    private final EquityDepotService equityDepotService;
    private final EquityDepotMapper equityDepotMapper;
    private final EquityUsePersonService equityUsePersonService;
    private final GoodsEquityService goodsEquityService;
    private final ServiceSessionService serviceSessionService;

    /** 获取当前登录 clientCode */
    private String currentClientCode() {
        Object code = StpKit.CLIENT.getSession().get("clientCode");
        if (code == null) {
            throw new IllegalStateException("未获取到登录身份");
        }
        return code.toString();
    }

    @Operation(summary = "我的权益列表（只返回已激活/使用中/已完成的）")
    @GetMapping("/my")
    public R<PageResult<EquityDepotVO>> myEquities(EquityDepotQueryDTO query) {
        query.setClientCode(currentClientCode());
        return R.ok(equityDepotService.page(query));
    }

    @Operation(summary = "权益详情")
    @GetMapping("/{equityCode}")
    public R<EquityDepotVO> detail(@PathVariable String equityCode) {
        EquityDepotVO vo = equityDepotService.getDetail(equityCode);
        // 越权防护：只能看自己的权益
        if (vo == null || !currentClientCode().equals(vo.getClientCode())) {
            return R.ok(null);
        }
        return R.ok(vo);
    }

    @Operation(summary = "权益下的使用人列表")
    @GetMapping("/{equityCode}/use-persons")
    public R<List<EquityUsePersonVO>> usePersons(@PathVariable String equityCode) {
        checkOwnership(equityCode);
        return R.ok(equityUsePersonService.listByEquity(equityCode));
    }

    /**
     * 权益下可用的服务项目 + 配额剩余次数（含结构化权益内容）。
     *
     * <p>返回每个 service_item 的名称、配额上限、已消费次数、剩余次数，
     * 以及入住权（保证/优先/优惠）、折扣率、单次使用规则——持卡人端可据此
     * 结构化展示权益卡内容，不再依赖商品描述文本。
     *
     * <p>配额口径：年度配额按激活周年重置（anchor=activateTime）；
     * share_mode=0（按人独立配额）时按 usePersonId 参数统计
     * （不传则取默认权益人），share_mode=1 为共享池。
     */
    @Operation(summary = "权益可用服务项目 + 配额剩余 + 权益内容")
    @GetMapping("/{equityCode}/service-items")
    public R<List<ClientServiceItemVO>> serviceItems(
            @PathVariable String equityCode,
            @RequestParam(required = false) Long usePersonId) {
        EquityDepot depot = checkOwnership(equityCode);
        List<GoodsEquityVO.ServiceItemRelVO> rels = goodsEquityService.listRelsByGoodsCode(depot.getGoodsCode());
        if (rels == null || rels.isEmpty()) {
            return R.ok(List.of());
        }
        LocalDate anchor = depot.getActivateTime() != null ? depot.getActivateTime().toLocalDate() : null;
        Long quotaPersonId = resolveQuotaPersonId(depot, usePersonId);
        List<ClientServiceItemVO> result = new ArrayList<>();
        for (GoodsEquityVO.ServiceItemRelVO rel : rels) {
            int quantity = rel.getQuantity() != null ? rel.getQuantity() : 1;
            int quotaType = rel.getQuotaType() != null ? rel.getQuotaType() : 2;
            int remaining = serviceSessionService.getRemainingQuota(
                    equityCode, rel.getItemCode(), quotaType, quantity, anchor, quotaPersonId);
            result.add(new ClientServiceItemVO(
                    rel.getItemCode(), rel.getItemName(), rel.getItemCategory(),
                    rel.getItemSubtype(), quantity, quotaType, quantity - remaining, remaining,
                    nz(rel.getAdmissionGuaranteed()), nz(rel.getAdmissionPriority()), nz(rel.getAdmissionDiscount()),
                    rel.getDiscountRate(), rel.getUsageRule(), rel.getNetworkScope()));
        }
        return R.ok(result);
    }

    /** 按人配额口径下解析统计的权益人：优先请求参数，缺省取默认权益人 */
    private Long resolveQuotaPersonId(EquityDepot depot, Long usePersonId) {
        boolean perPerson = depot.getShareMode() != null && depot.getShareMode() == 0;
        if (!perPerson) {
            return null; // 共享池口径
        }
        if (usePersonId != null) {
            return usePersonId;
        }
        List<EquityUsePersonVO> persons = equityUsePersonService.listByEquity(depot.getEquityCode());
        if (persons != null) {
            for (EquityUsePersonVO p : persons) {
                if (p.getIsDefaultHolder() != null && p.getIsDefaultHolder() == 1) {
                    return p.getId();
                }
            }
        }
        return null;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * 持卡人为权益人发起服务请求（核心入口）。
     *
     * <p>校验链：权益归属 → 权益状态 → 使用人归属 → 配额剩余 → 创建 session（状态=1 待分配）。
     * 管家在 admin 端收到待分配的 session 后进行受理（分配管家→提交需求→方案→安排→开始服务→完成）。
     */
    @Operation(summary = "发起服务请求")
    @PostMapping("/service-request")
    public R<String> createServiceRequest(@RequestBody @Valid ClientServiceRequestDTO dto) {
        // 1. 校验权益归属 + 状态
        EquityDepot depot = checkOwnership(dto.getEquityCode());
        String clientCode = currentClientCode();

        // 2. 查 rel 配置，校验服务项目属于该商品 + 拿到 quotaType
        List<GoodsEquityVO.ServiceItemRelVO> rels = goodsEquityService.listRelsByGoodsCode(depot.getGoodsCode());
        GoodsEquityVO.ServiceItemRelVO targetRel = null;
        if (rels != null) {
            for (GoodsEquityVO.ServiceItemRelVO rel : rels) {
                if (dto.getItemCode().equals(rel.getItemCode())) {
                    targetRel = rel;
                    break;
                }
            }
        }
        if (targetRel == null) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.BUSINESS,
                    "该权益不包含此服务项目: " + dto.getItemCode());
        }

        // 3. 配额校验（年度按激活周年；share_mode=0 按人独立配额）
        int quantity = targetRel.getQuantity() != null ? targetRel.getQuantity() : 1;
        int quotaType = targetRel.getQuotaType() != null ? targetRel.getQuotaType() : 2;

        // 4. 校验使用人归属该权益（防伪造 usePersonId）
        Long usePersonIdVal;
        String usePersonName = dto.getUsePersonId();
        try {
            usePersonIdVal = Long.parseLong(dto.getUsePersonId());
        } catch (NumberFormatException e) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.BUSINESS, "权益人ID格式错误");
        }
        List<EquityUsePersonVO> persons = equityUsePersonService.listByEquity(dto.getEquityCode());
        boolean personValid = false;
        if (persons != null) {
            for (EquityUsePersonVO p : persons) {
                if (p.getId() != null && p.getId().equals(usePersonIdVal)) {
                    personValid = true;
                    usePersonName = p.getUsePersonName();
                    break;
                }
            }
        }
        if (!personValid) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.BUSINESS, "权益人不属于当前权益");
        }

        LocalDate anchor = depot.getActivateTime() != null ? depot.getActivateTime().toLocalDate() : null;
        Long quotaPersonId = (depot.getShareMode() != null && depot.getShareMode() == 0)
                ? usePersonIdVal : null;
        int remaining = serviceSessionService.getRemainingQuota(
                dto.getEquityCode(), dto.getItemCode(), quotaType, quantity, anchor, quotaPersonId);
        if (remaining <= 0) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.BUSINESS,
                    "服务项目配额已用尽: " + targetRel.getItemName());
        }

        // 5. 构造 session DTO 创建
        Integer quotaYear = anchor != null && quotaType == 2
                ? QuotaYears.benefitYear(anchor, LocalDate.now(ZoneId.of("Asia/Shanghai")))
                : null;
        ServiceSessionCreateDTO sessionDTO = new ServiceSessionCreateDTO();
        sessionDTO.setEquityCode(dto.getEquityCode());
        sessionDTO.setItemCode(dto.getItemCode());
        sessionDTO.setUsePersonId(usePersonIdVal);
        sessionDTO.setClientCode(clientCode);
        sessionDTO.setQuotaType(quotaType);
        sessionDTO.setQuotaYear(quotaYear);
        sessionDTO.setServiceTitle(targetRel.getItemName() + " - " + usePersonName);
        sessionDTO.setServiceDescription(dto.getDemandDesc());
        sessionDTO.setSourceType(2); // 2=客户主动
        sessionDTO.setAgentCode(depot.getAgentCode());
        sessionDTO.setChannelCode(depot.getChannelCode());
        sessionDTO.setRemark("客户端发起（权益：" + dto.getEquityCode()
                + "，项目：" + targetRel.getItemName()
                + "，权益人：" + usePersonName + "(" + usePersonIdVal + ")）");

        String sessionCode = serviceSessionService.create(sessionDTO);
        return R.ok(sessionCode);
    }

    /**
     * 持卡人激活权益（输入卡面激活码）。
     *
     * <p>clientCode 强制取登录态，防越权激活他人权益。激活后后端自动建占位使用人，
     * 前端引导用户在「权益人管理」补全真实老人信息。
     */
    @Operation(summary = "激活权益（输入激活码）")
    @PostMapping("/activate")
    public R<EquityDepotVO> activate(@RequestBody @Valid ClientActivateDTO dto) {
        String clientCode = currentClientCode();
        ActivateDTO activateDTO = new ActivateDTO();
        activateDTO.setCarrierType(1); // 1=权益卡（按 activateCode）
        activateDTO.setActivateCode(dto.getActivateCode());
        activateDTO.setClientCode(clientCode); // 强制登录态，覆盖任何前端值
        // 客户全名从登录态取（login 时存 username 快照），填充 equity_activate.client_full_name（NOT NULL）
        Object fullName = StpKit.CLIENT.getSession().get("clientFullName");
        activateDTO.setClientFullName(fullName != null ? fullName.toString() : clientCode);
        activateDTO.setActivateChannel(3);     // 3=H5（小程序环境由前端传 deviceInfo 时再细化）
        // service.activate 返回 activateRecordCode（非 equityCode），用 activateCode 反查 equityCode 取详情
        equityDepotService.activate(activateDTO);
        EquityDepot activated = equityDepotMapper.selectOne(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getActivateCode, dto.getActivateCode())
                .last("LIMIT 1"));
        if (activated == null) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.NOT_FOUND, "激活失败，权益未找到");
        }
        return R.ok(equityDepotService.getDetail(activated.getEquityCode()));
    }

    /** 校验权益归属当前 client（越权防护），返回权益实体 */
    private EquityDepot checkOwnership(String equityCode) {
        EquityDepot depot = equityDepotMapper.selectOne(new LambdaQueryWrapper<EquityDepot>()
                .eq(EquityDepot::getEquityCode, equityCode)
                .last("LIMIT 1"));
        if (depot == null || !currentClientCode().equals(depot.getClientCode())) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.NOT_FOUND, "权益不存在或无权访问");
        }
        // 只允许已激活(2)或使用中(3)的权益操作
        if (depot.getEquityStatus() != EquityEvent.STATUS_ACTIVATED
                && depot.getEquityStatus() != EquityEvent.STATUS_IN_USE) {
            throw new com.dayan.common.core.exception.BusinessException(
                    com.dayan.common.core.exception.ErrorCode.BUSINESS, "权益当前状态不支持此操作");
        }
        return depot;
    }

    /** 客户端服务项目 VO（含配额剩余 + 结构化权益内容） */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ClientServiceItemVO {
        /** 服务项目编码 */
        private String itemCode;
        /** 服务项目名称 */
        private String itemName;
        /** 大类（1=安排权益,2=费用权益） */
        private Integer itemCategory;
        /** 子类（1=旅游短居,2=活力长居,3=照护长居） */
        private Integer itemSubtype;
        /** 配额上限 */
        private Integer quantity;
        /** 配额周期（1=终身,2=年度（按激活周年重置）） */
        private Integer quotaType;
        /** 已消费次数 */
        private Integer consumed;
        /** 剩余次数 */
        private Integer remaining;
        /** 保证入住权（0=无,1=有；长居/照护） */
        private Integer admissionGuaranteed;
        /** 优先入住权（0=无,1=有） */
        private Integer admissionPriority;
        /** 优惠入住权/旅居优惠权（0=无,1=有） */
        private Integer admissionDiscount;
        /** 优惠折扣率（90.00=门市价9折；null=按协议未定） */
        private java.math.BigDecimal discountRate;
        /** 单次使用规则（随心住类：晚数/间数/人数/预订/预定金/取消政策/黑名单） */
        private com.dayan.goods.model.UsageRule usageRule;
        /** 服务网络范围（null=业态全部机构；custom=自选范围） */
        private com.dayan.goods.model.NetworkScope networkScope;
    }
}
