package com.dayan.equity.enums;

/**
 * 权益状态机（EQUITY_SM）事件常量。
 *
 * <p>8 态 12 规则，规则已存在于 {@code db/migration/seed/state_machine_seed.sql}：
 * <ul>
 *   <li>{@link #OUTBOUND}        0→1（出库）</li>
 *   <li>{@link #ACTIVATE}        1→2（激活）</li>
 *   <li>{@link #START_SERVICE}   2→3（发起服务）</li>
 *   <li>{@link #END_SERVICE}     3→2（服务结束恢复）</li>
 *   <li>{@link #COMPLETE}        3→4（权益完成）</li>
 *   <li>{@link #SHELF_EXPIRE}    0→5（上架过期）</li>
 *   <li>{@link #EXPIRE}          1→5 / 2→5（有效期过期）</li>
 *   <li>{@link #VOID}            0→6 / 1→6（作废）</li>
 *   <li>{@link #CHANGE_HOLDER}   2→7（发起更换权益人）</li>
 *   <li>{@link #CHANGE_DONE}     7→2（更换完成；回滚也复用此事件将 7→2）</li>
 * </ul>
 *
 * <p>状态码与字典 {@code equity_status} 对齐：
 * 0=库存中 / 1=已出库 / 2=已激活 / 3=使用中 / 4=已完成 / 5=已过期 / 6=已作废 / 7=更换权益人中。
 */
public final class EquityEvent {

    private EquityEvent() {
    }

    /** 状态机域标识（machine_code） */
    public static final String DOMAIN = "EQUITY_SM";

    /** 出库：0→1 */
    public static final String OUTBOUND = "outbound";
    /** 激活：1→2 */
    public static final String ACTIVATE = "activate";
    /** 发起服务：2→3 */
    public static final String START_SERVICE = "start_service";
    /** 服务结束恢复：3→2 */
    public static final String END_SERVICE = "end_service";
    /** 权益完成：3→4 */
    public static final String COMPLETE = "complete";
    /** 上架过期：0→5 */
    public static final String SHELF_EXPIRE = "shelf_expire";
    /** 有效期过期：1→5 / 2→5 */
    public static final String EXPIRE = "expire";
    /** 作废：0→6 / 1→6 */
    public static final String VOID = "void";
    /** 发起更换权益人：2→7 */
    public static final String CHANGE_HOLDER = "change_holder";
    /** 更换完成（及回滚复用）：7→2 */
    public static final String CHANGE_DONE = "change_done";

    // ====== 状态码常量 ======

    /** 库存中 */
    public static final int STATUS_STOCK = 0;
    /** 已出库 */
    public static final int STATUS_OUTBOUND = 1;
    /** 已激活 */
    public static final int STATUS_ACTIVATED = 2;
    /** 使用中 */
    public static final int STATUS_IN_USE = 3;
    /** 已完成 */
    public static final int STATUS_COMPLETED = 4;
    /** 已过期 */
    public static final int STATUS_EXPIRED = 5;
    /** 已作废 */
    public static final int STATUS_VOID = 6;
    /** 更换权益人中 */
    public static final int STATUS_CHANGING_HOLDER = 7;

    /** change_holder 记录状态：0 待处理 */
    public static final int CHANGE_STATUS_PENDING = 0;
    /** change_holder 记录状态：1 已完成 */
    public static final int CHANGE_STATUS_DONE = 1;
    /** change_holder 记录状态：2 已回滚 */
    public static final int CHANGE_STATUS_ROLLBACK = 2;
}
