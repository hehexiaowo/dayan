package com.dayan.service.enums;

/**
 * 服务会话状态机（SERVICE_SESSION_SM）事件常量。
 *
 * <p>7 态 9 规则，规则已存在于 {@code db/migration/seed/state_machine_seed.sql}：
 * <ul>
 *   <li>{@link #ASSIGN_BUTLER}    1→2（分配管家）</li>
 *   <li>{@link #SUBMIT_DEMAND}    2→3（提交需求，进入方案）</li>
 *   <li>{@link #CONFIRM_SOLUTION} 3→4（确认方案）</li>
 *   <li>{@link #REJECT_SOLUTION}  3→2（驳回方案，退回需求）</li>
 *   <li>{@link #START_SERVICE}    4→5（开始服务）</li>
 *   <li>{@link #FINISH}           5→6（完成）</li>
 *   <li>{@link #CANCEL}           1→7 / 2→7 / 5→7（取消，仅特定态可取消）</li>
 * </ul>
 *
 * <p>状态码与字典 {@code service_session.session_status} 对齐：
 * 1=待分配 / 2=待收集 / 3=方案中 / 4=安排中 / 5=服务中 / 6=已完成 / 7=已取消
 * （以状态机种子的数值流转为准；DDL 行内注释仅为描述差异）。
 *
 * <p>子状态（sub_status，独立于状态机，由应用层直接 UPDATE）：
 * normal/hold/urgent/reassign/refund_review/refund_done/interrupted。
 */
public final class ServiceSessionEvent {

    private ServiceSessionEvent() {
    }

    /** 状态机域标识（machine_code） */
    public static final String DOMAIN = "SERVICE_SESSION_SM";

    /** 分配管家：1→2 */
    public static final String ASSIGN_BUTLER = "assign_butler";
    /** 提交需求：2→3 */
    public static final String SUBMIT_DEMAND = "submit_demand";
    /** 确认方案：3→4 */
    public static final String CONFIRM_SOLUTION = "confirm_solution";
    /** 驳回方案：3→2 */
    public static final String REJECT_SOLUTION = "reject_solution";
    /** 开始服务：4→5 */
    public static final String START_SERVICE = "start_service";
    /** 完成服务：5→6 */
    public static final String FINISH = "finish";
    /** 取消：1→7 / 2→7 / 5→7 */
    public static final String CANCEL = "cancel";

    // ====== 状态码常量 ======

    /** 待分配 */
    public static final int STATUS_PENDING_ASSIGN = 1;
    /** 待收集 */
    public static final int STATUS_PENDING_COLLECT = 2;
    /** 方案中 */
    public static final int STATUS_SOLUTION = 3;
    /** 安排中 */
    public static final int STATUS_ARRANGE = 4;
    /** 服务中 */
    public static final int STATUS_IN_SERVICE = 5;
    /** 已完成（终态） */
    public static final int STATUS_COMPLETED = 6;
    /** 已取消（终态） */
    public static final int STATUS_CANCELLED = 7;

    // ====== 子状态常量（sub_status 字段，不经状态机） ======

    /** 正常（默认） */
    public static final String SUB_NORMAL = "normal";
    /** 暂停 */
    public static final String SUB_HOLD = "hold";
    /** 紧急（SLA 超时升级） */
    public static final String SUB_URGENT = "urgent";
    /** 改派管家 */
    public static final String SUB_REASSIGN = "reassign";
    /** 退款审核中 */
    public static final String SUB_REFUND_REVIEW = "refund_review";
    /** 退款完成 */
    public static final String SUB_REFUND_DONE = "refund_done";
    /** 中断 */
    public static final String SUB_INTERRUPTED = "interrupted";
}
