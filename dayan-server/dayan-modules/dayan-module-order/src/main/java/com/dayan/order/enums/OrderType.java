package com.dayan.order.enums;

/**
 * 订单类型常量（order_status_change_record.order_type 字段值）。
 *
 * <p>与各表 order_type 字段值对齐：order_equity 隐含 1、order_scene=2、order_course=3、order_sojourn=4。
 */
public final class OrderType {

    private OrderType() {
    }

    /** 权益采购订单 */
    public static final int EQUITY = 1;
    /** 场景报名订单 */
    public static final int SCENE = 2;
    /** 课程购买订单 */
    public static final int COURSE = 3;
    /** 旅游短居预订订单 */
    public static final int SOJOURN = 4;
}
