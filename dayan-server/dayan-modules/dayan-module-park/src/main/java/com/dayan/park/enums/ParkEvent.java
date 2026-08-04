package com.dayan.park.enums;

/**
 * 养老机构状态机（PARK_SM）事件常量。
 *
 * <p>对应 {@code system_state_machine} 表 machine_code=PARK_SM 的 5 条流转规则：
 * <ul>
 *   <li>{@link #APPROVE}  0 待审核 -&gt; 1 已上线</li>
 *   <li>{@link #OFFLINE}  1 已上线 -&gt; 2 已下架</li>
 *   <li>{@link #ONLINE}   2 已下架 -&gt; 1 已上线</li>
 *   <li>{@link #SUSPEND}  1 已上线 -&gt; 3 暂停营业</li>
 *   <li>{@link #RESUME}   3 暂停营业 -&gt; 1 已上线</li>
 * </ul>
 *
 * <p>状态字段：park_info.operate_status（0=待审核 / 1=已上线 / 2=已下架 / 3=暂停营业）。
 */
public final class ParkEvent {

    private ParkEvent() {
    }

    /** 审核通过上线：0 -&gt; 1 */
    public static final String APPROVE = "approve";
    /** 下架：1 -&gt; 2 */
    public static final String OFFLINE = "offline";
    /** 重新上线：2 -&gt; 1 */
    public static final String ONLINE = "online";
    /** 暂停营业：1 -&gt; 3 */
    public static final String SUSPEND = "suspend";
    /** 恢复营业：3 -&gt; 1 */
    public static final String RESUME = "resume";
}
