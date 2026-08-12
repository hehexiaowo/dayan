package com.dayan.service.controller.client;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.security.StpKit;
import com.dayan.service.dto.CancelSessionDTO;
import com.dayan.service.dto.ClientEvaluationDTO;
import com.dayan.service.dto.ServiceEvaluationCreateDTO;
import com.dayan.service.dto.ServiceSessionQueryDTO;
import com.dayan.service.service.ServiceEquityArrangeService;
import com.dayan.service.service.ServiceEquityDemandService;
import com.dayan.service.service.ServiceEquitySolutionService;
import com.dayan.service.service.ServiceEvaluationService;
import com.dayan.service.service.ServiceSessionService;
import com.dayan.service.service.ServiceVisitRecordService;
import com.dayan.service.vo.ServiceEquityArrangeVO;
import com.dayan.service.vo.ServiceEquityDemandVO;
import com.dayan.service.vo.ServiceEquitySolutionVO;
import com.dayan.service.vo.ServiceSessionVO;
import com.dayan.service.vo.ServiceTimelineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务会话 - client 端接口（持卡人视角）。
 *
 * <p>路径：{@code /client-api/service-sessions/*}。
 * 持卡人可查看自己的服务会话列表/详情、跟进服务进度时间线，并在状态机关键节点操作
 * （确认方案 / 驳回方案 / 取消服务 / 评价服务）。发起请求请用
 * {@code POST /client-api/equity/service-request}（在 equity controller）。
 */
@Tag(name = "Client 服务会话")
@RestController
@RequestMapping("/service-sessions")
@RequiredArgsConstructor
public class ClientServiceSessionController {

    private final ServiceSessionService serviceSessionService;
    private final ServiceEquityDemandService demandService;
    private final ServiceEquitySolutionService solutionService;
    private final ServiceEquityArrangeService arrangeService;
    private final ServiceVisitRecordService visitService;
    private final ServiceEvaluationService evaluationService;

    /** 获取当前登录 clientCode */
    private String currentClientCode() {
        Object code = StpKit.CLIENT.getSession().get("clientCode");
        if (code == null) {
            throw new IllegalStateException("未获取到登录身份");
        }
        return code.toString();
    }

    /** 校验会话归属当前 client，返回 VO */
    private ServiceSessionVO checkSessionOwnership(String sessionCode) {
        ServiceSessionVO vo = serviceSessionService.getDetail(sessionCode);
        if (vo == null || !currentClientCode().equals(vo.getClientCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在或无权访问");
        }
        return vo;
    }

    @Operation(summary = "我的服务会话列表")
    @GetMapping("/my")
    public R<PageResult<ServiceSessionVO>> mySessions(ServiceSessionQueryDTO query) {
        query.setClientCode(currentClientCode());
        return R.ok(serviceSessionService.page(query));
    }

    @Operation(summary = "服务会话详情")
    @GetMapping("/{sessionCode}")
    public R<ServiceSessionVO> detail(@PathVariable String sessionCode) {
        return R.ok(checkSessionOwnership(sessionCode));
    }

    @Operation(summary = "服务进度时间线")
    @GetMapping("/{sessionCode}/timeline")
    public R<ServiceTimelineVO> timeline(@PathVariable String sessionCode) {
        checkSessionOwnership(sessionCode);
        ServiceTimelineVO vo = new ServiceTimelineVO();
        vo.setDemands(demandToNodes(demandService.listBySession(sessionCode)));
        vo.setSolutions(solutionToNodes(solutionService.listBySession(sessionCode)));
        vo.setArranges(arrangeToNodes(arrangeService.listBySession(sessionCode)));
        // ServiceVisitRecordQueryDTO 暂无 sessionCode 字段，无法按会话过滤，故 visits 暂返回空
        vo.setVisits(new ArrayList<>());
        return R.ok(vo);
    }

    @Operation(summary = "确认方案（3→4）")
    @PostMapping("/{sessionCode}/confirm-solution")
    public R<Void> confirmSolution(@PathVariable String sessionCode) {
        checkSessionOwnership(sessionCode);
        serviceSessionService.confirmSolution(sessionCode);
        return R.ok();
    }

    @Operation(summary = "驳回方案（3→2）")
    @PostMapping("/{sessionCode}/reject")
    public R<Void> rejectSolution(@PathVariable String sessionCode) {
        checkSessionOwnership(sessionCode);
        serviceSessionService.rejectSolution(sessionCode);
        return R.ok();
    }

    @Operation(summary = "取消服务（1/2/5→7）")
    @PostMapping("/{sessionCode}/cancel")
    public R<Void> cancel(@PathVariable String sessionCode, @RequestBody CancelSessionDTO dto) {
        checkSessionOwnership(sessionCode);
        dto.setSessionCode(sessionCode);
        serviceSessionService.cancel(dto);
        return R.ok();
    }

    @Operation(summary = "评价服务（完成后）")
    @PostMapping("/{sessionCode}/evaluate")
    public R<Long> evaluate(@PathVariable String sessionCode, @RequestBody @Valid ClientEvaluationDTO dto) {
        ServiceSessionVO session = checkSessionOwnership(sessionCode);
        if (session.getSessionStatus() == null || session.getSessionStatus() != 6) {
            throw new BusinessException(ErrorCode.BUSINESS, "仅已完成的服务可评价");
        }
        ServiceEvaluationCreateDTO eval = new ServiceEvaluationCreateDTO();
        eval.setSessionCode(sessionCode);
        eval.setClientCode(currentClientCode()); // 强制登录态
        eval.setAttitudeRating(dto.getAttitudeRating());
        eval.setProfessionalRating(dto.getProfessionalRating());
        eval.setResponsivenessRating(dto.getResponsivenessRating());
        eval.setSatisfactionRating(dto.getSatisfactionRating());
        eval.setContent(dto.getContent());
        return R.ok(evaluationService.create(eval));
    }

    // ====== timeline 节点映射 ======

    private List<ServiceTimelineVO.Node> demandToNodes(List<ServiceEquityDemandVO> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream().map(d -> {
            ServiceTimelineVO.Node n = new ServiceTimelineVO.Node();
            n.setType("demand");
            n.setTitle(d.getUsePersonName() != null ? d.getUsePersonName() + " 的需求" : "需求收集");
            n.setContent(d.getDemandSummary());
            n.setTime(d.getCollectTime() != null ? d.getCollectTime() : d.getCreatedAt());
            n.setStatus(d.getStatus());
            return n;
        }).collect(Collectors.toList());
    }

    private List<ServiceTimelineVO.Node> solutionToNodes(List<ServiceEquitySolutionVO> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream().map(s -> {
            ServiceTimelineVO.Node n = new ServiceTimelineVO.Node();
            n.setType("solution");
            n.setTitle(s.getSolutionName());
            n.setContent(s.getPlanSummary());
            n.setTime(s.getPresentationTime() != null ? s.getPresentationTime() : s.getCreatedAt());
            n.setStatus(s.getIsAccepted());
            return n;
        }).collect(Collectors.toList());
    }

    private List<ServiceTimelineVO.Node> arrangeToNodes(List<ServiceEquityArrangeVO> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream().map(a -> {
            ServiceTimelineVO.Node n = new ServiceTimelineVO.Node();
            n.setType("arrange");
            n.setTitle(a.getParkFullName() != null ? a.getParkFullName() + " 安排" : "全程安排");
            n.setContent(a.getProgressNotes());
            n.setTime(a.getConfirmTime() != null ? a.getConfirmTime()
                    : (a.getCompleteTime() != null ? a.getCompleteTime() : a.getCreatedAt()));
            n.setStatus(a.getStatus());
            return n;
        }).collect(Collectors.toList());
    }
}
