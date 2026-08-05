package com.dayan.job.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.order.dto.OrderCancelDTO;
import com.dayan.order.entity.OrderCourse;
import com.dayan.order.entity.OrderEquity;
import com.dayan.order.entity.OrderScene;
import com.dayan.order.entity.OrderSojourn;
import com.dayan.order.enums.OrderEvent;
import com.dayan.order.mapper.OrderCourseMapper;
import com.dayan.order.mapper.OrderEquityMapper;
import com.dayan.order.mapper.OrderSceneMapper;
import com.dayan.order.mapper.OrderSojournMapper;
import com.dayan.order.service.OrderCourseService;
import com.dayan.order.service.OrderEquityService;
import com.dayan.order.service.OrderSceneService;
import com.dayan.order.service.OrderSojournService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时取消定时任务。
 *
 * <p>每 5 分钟扫描待支付订单（order_status={@link OrderEvent#STATUS_PENDING_PAY}），
 * 经 {@code ORDER_SM} 状态机 {@code cancel} 事件合法转移至"已取消"（状态 {@link OrderEvent#STATUS_CANCELLED}）。
 *
 * <p>超时判定分两类（与 DDL 字段对齐）：
 * <ul>
 *   <li><b>order_equity</b>：有独立 {@code expire_time} 列（创建时默认 now+30min），用 {@code expire_time < now} 精确判定</li>
 *   <li><b>order_scene / order_course / order_sojourn</b>：无 {@code expire_time} 列，回退用
 *       {@code created_at < now - 30min} 判定（依赖业务约定：30 分钟未支付视为放弃）</li>
 * </ul>
 *
 * <p>分批处理（每批 {@link #BATCH_SIZE} 条），逐条经 {@code OrderXxxService.cancel} 走状态机，
 * 保证每条流转合法并写 status_log。单条失败仅记录日志、不中断整批。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    /** 单批处理条数（避免大事务、控制内存） */
    private static final int BATCH_SIZE = 500;
    /** 非 equity 订单的默认超时分钟数（无 expire_time 列时回退使用） */
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;
    /** 系统自动取消原因 */
    private static final String CANCEL_REASON = "超时未支付，系统自动取消";
    /** 系统操作人标识 */
    private static final String OPERATOR_CODE = "system";
    private static final String OPERATOR_NAME = "超时取消任务";
    private static final String OPERATOR_TYPE = "system";

    private final OrderEquityMapper orderEquityMapper;
    private final OrderSceneMapper orderSceneMapper;
    private final OrderCourseMapper orderCourseMapper;
    private final OrderSojournMapper orderSojournMapper;

    private final OrderEquityService orderEquityService;
    private final OrderSceneService orderSceneService;
    private final OrderCourseService orderCourseService;
    private final OrderSojournService orderSojournService;

    /**
     * 每 5 分钟执行（cron：分 时 日 月 周）。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelTimeoutOrders() {
        long start = System.currentTimeMillis();
        try {
            int equityCount = scanEquity();
            int sceneCount = scanScene();
            int courseCount = scanCourse();
            int sojournCount = scanSojourn();
            int total = equityCount + sceneCount + courseCount + sojournCount;
            log.info("[订单超时取消] 扫描完成，权益 {} / 场景 {} / 课程 {} / 旅居 {} / 合计 {} 笔，耗时 {}ms",
                    equityCount, sceneCount, courseCount, sojournCount, total,
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[订单超时取消] 执行失败", e);
        }
    }

    /**
     * 扫描权益订单：order_status=0 且 expire_time < now。
     *
     * <p>order_equity 表有独立 expire_time 列（创建时 now+30min），用其精确判定。
     */
    private int scanEquity() {
        LocalDateTime now = LocalDateTime.now();
        int processed = 0;
        while (true) {
            List<OrderEquity> list = orderEquityMapper.selectList(
                    new LambdaQueryWrapper<OrderEquity>()
                            .eq(OrderEquity::getOrderStatus, OrderEvent.STATUS_PENDING_PAY)
                            .lt(OrderEquity::getExpireTime, now)
                            .last("LIMIT " + BATCH_SIZE));
            if (list.isEmpty()) {
                break;
            }
            for (OrderEquity order : list) {
                try {
                    orderEquityService.cancel(buildCancelDTO(order.getOrderCode()));
                    processed++;
                } catch (Exception e) {
                    log.warn("[订单超时取消-权益] orderCode={} 取消失败: {}",
                            order.getOrderCode(), e.getMessage());
                }
            }
            if (list.size() < BATCH_SIZE) {
                break;
            }
        }
        return processed;
    }

    /**
     * 扫描场景订单：order_status=0 且 created_at < now - 30min。
     *
     * <p>order_scene 表无 expire_time 列，回退用 created_at 判定。
     */
    private int scanScene() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(DEFAULT_TIMEOUT_MINUTES);
        int processed = 0;
        while (true) {
            List<OrderScene> list = orderSceneMapper.selectList(
                    new LambdaQueryWrapper<OrderScene>()
                            .eq(OrderScene::getOrderStatus, OrderEvent.STATUS_PENDING_PAY)
                            .lt(OrderScene::getCreatedAt, threshold)
                            .last("LIMIT " + BATCH_SIZE));
            if (list.isEmpty()) {
                break;
            }
            for (OrderScene order : list) {
                try {
                    orderSceneService.cancel(buildCancelDTO(order.getOrderCode()));
                    processed++;
                } catch (Exception e) {
                    log.warn("[订单超时取消-场景] orderCode={} 取消失败: {}",
                            order.getOrderCode(), e.getMessage());
                }
            }
            if (list.size() < BATCH_SIZE) {
                break;
            }
        }
        return processed;
    }

    /**
     * 扫描课程订单：order_status=0 且 created_at < now - 30min。
     */
    private int scanCourse() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(DEFAULT_TIMEOUT_MINUTES);
        int processed = 0;
        while (true) {
            List<OrderCourse> list = orderCourseMapper.selectList(
                    new LambdaQueryWrapper<OrderCourse>()
                            .eq(OrderCourse::getOrderStatus, OrderEvent.STATUS_PENDING_PAY)
                            .lt(OrderCourse::getCreatedAt, threshold)
                            .last("LIMIT " + BATCH_SIZE));
            if (list.isEmpty()) {
                break;
            }
            for (OrderCourse order : list) {
                try {
                    orderCourseService.cancel(buildCancelDTO(order.getOrderCode()));
                    processed++;
                } catch (Exception e) {
                    log.warn("[订单超时取消-课程] orderCode={} 取消失败: {}",
                            order.getOrderCode(), e.getMessage());
                }
            }
            if (list.size() < BATCH_SIZE) {
                break;
            }
        }
        return processed;
    }

    /**
     * 扫描旅居订单：order_status=0 且 created_at < now - 30min。
     */
    private int scanSojourn() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(DEFAULT_TIMEOUT_MINUTES);
        int processed = 0;
        while (true) {
            List<OrderSojourn> list = orderSojournMapper.selectList(
                    new LambdaQueryWrapper<OrderSojourn>()
                            .eq(OrderSojourn::getOrderStatus, OrderEvent.STATUS_PENDING_PAY)
                            .lt(OrderSojourn::getCreatedAt, threshold)
                            .last("LIMIT " + BATCH_SIZE));
            if (list.isEmpty()) {
                break;
            }
            for (OrderSojourn order : list) {
                try {
                    orderSojournService.cancel(buildCancelDTO(order.getOrderCode()));
                    processed++;
                } catch (Exception e) {
                    log.warn("[订单超时取消-旅居] orderCode={} 取消失败: {}",
                            order.getOrderCode(), e.getMessage());
                }
            }
            if (list.size() < BATCH_SIZE) {
                break;
            }
        }
        return processed;
    }

    /**
     * 构造系统取消 DTO（统一操作人=system、原因=超时未支付）。
     */
    private OrderCancelDTO buildCancelDTO(String orderCode) {
        OrderCancelDTO dto = new OrderCancelDTO();
        dto.setOrderCode(orderCode);
        dto.setCancelReason(CANCEL_REASON);
        dto.setOperatorCode(OPERATOR_CODE);
        dto.setOperatorName(OPERATOR_NAME);
        dto.setOperatorType(OPERATOR_TYPE);
        return dto;
    }
}
