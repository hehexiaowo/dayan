package com.dayan.content.enums;

/**
 * 内容审核状态机（CONTENT_SM）事件常量。
 *
 * <p>5 态 4 规则，规则已存在于 {@code db/migration/seed/state_machine_seed.sql}：
 * <ul>
 *   <li>{@link #SUBMIT}        0→1（提交审核）</li>
 *   <li>{@link #AUDIT_PASS}    1→2（审核通过）</li>
 *   <li>{@link #AUDIT_REJECT}  1→3（审核驳回）</li>
 *   <li>{@link #OFFLINE}       2→4（下线）</li>
 * </ul>
 *
 * <p>状态码与字典/DDL {@code content_info.content_status} 注释对齐：
 * 0=草稿 / 1=待审核 / 2=审核通过 / 3=审核驳回 / 4=已下线。
 *
 * <p>注：{@code publish} 为 2→2 自环（仅置 publishTime，不改 contentStatus），
 * 不纳入状态机规则表达，保留在业务层硬编码处理。
 */
public final class ContentEvent {

    private ContentEvent() {
    }

    /** 状态机域标识（machine_code） */
    public static final String DOMAIN = "CONTENT_SM";

    /** 提交审核：0→1 */
    public static final String SUBMIT = "submit";
    /** 审核通过：1→2 */
    public static final String AUDIT_PASS = "audit_pass";
    /** 审核驳回：1→3 */
    public static final String AUDIT_REJECT = "audit_reject";
    /** 下线：2→4 */
    public static final String OFFLINE = "offline";

    // ====== 状态码常量 ======

    /** 草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 待审核 */
    public static final int STATUS_PENDING = 1;
    /** 审核通过 */
    public static final int STATUS_PASS = 2;
    /** 审核驳回 */
    public static final int STATUS_REJECT = 3;
    /** 已下线 */
    public static final int STATUS_OFFLINE = 4;
}
