package com.dayan.order.enums;

/**
 * 订单状态机（ORDER_SM）事件常量。
 *
 * <p>8 态 12 规则，规则已存在于 {@code db/migration/seed/state_machine_seed.sql}：
 * <ul>
 *   <li>{@link #PAY}              0→1（支付成功）</li>
 *   <li>{@link #CANCEL}           0→5 / 6→5（用户取消或退款流程取消）</li>
 *   <li>{@link #PARTIAL_DELIVER}  1→2（部分发放，权益订单批量出库部分完成）</li>
 *   <li>{@link #DELIVER}          1→3 / 2→3（全部发放完成）</li>
 *   <li>{@link #COMPLETE}         3→4（业务完结）</li>
 *   <li>{@link #REFUND_APPLY}     1→6 / 2→6 / 3→6（申请退款）</li>
 *   <li>{@link #REFUND_DONE}      6→7（退款到账）</li>
 *   <li>{@link #REFUND_REJECT}    6→1（退款驳回恢复已支付）</li>
 * </ul>
 *
 * <p>状态码与字典/DDL {@code order_status} 对齐：
 * 0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款。
 */
public final class OrderEvent {

    private OrderEvent() {
    }

    /** 状态机域标识（machine_code） */
    public static final String DOMAIN = "ORDER_SM";

    /** 支付成功：0→1 */
    public static final String PAY = "pay";
    /** 取消：0→5 / 6→5 */
    public static final String CANCEL = "cancel";
    /** 部分发放：1→2 */
    public static final String PARTIAL_DELIVER = "partial_deliver";
    /** 全部发放：1→3 / 2→3 */
    public static final String DELIVER = "deliver";
    /** 业务完结：3→4 */
    public static final String COMPLETE = "complete";
    /** 申请退款：1→6 / 2→6 / 3→6 */
    public static final String REFUND_APPLY = "refund_apply";
    /** 退款到账：6→7 */
    public static final String REFUND_DONE = "refund_done";
    /** 退款驳回恢复：6→1 */
    public static final String REFUND_REJECT = "refund_reject";

    // ====== 状态码常量 ======

    /** 待支付 */
    public static final int STATUS_PENDING_PAY = 0;
    /** 已支付 */
    public static final int STATUS_PAID = 1;
    /** 部分发放 */
    public static final int STATUS_PARTIAL_DELIVERED = 2;
    /** 已发放 */
    public static final int STATUS_DELIVERED = 3;
    /** 已完成 */
    public static final int STATUS_COMPLETED = 4;
    /** 已取消 */
    public static final int STATUS_CANCELLED = 5;
    /** 退款中 */
    public static final int STATUS_REFUNDING = 6;
    /** 已退款 */
    public static final int STATUS_REFUNDED = 7;
}
