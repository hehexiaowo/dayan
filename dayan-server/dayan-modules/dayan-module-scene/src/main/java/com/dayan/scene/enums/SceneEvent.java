package com.dayan.scene.enums;

/**
 * 场景审核状态机（SCENE_SM）事件常量。
 *
 * <p>4 态 3 规则，规则已存在于 {@code db/migration/seed/state_machine_seed.sql}：
 * <ul>
 *   <li>{@link #SHELVES}     0→1（上架）</li>
 *   <li>{@link #OFFSHELVES}  1→2（下架）</li>
 *   <li>{@link #RESHELVES}   2→1（重新上架）</li>
 * </ul>
 *
 * <p>状态码与字典/DDL {@code scene_info.scene_status} 注释对齐：
 * 0=草稿 / 1=已上架 / 2=已下架 / 3=已满期。
 *
 * <p>注：{@code submit}/{@code audit} 仅维护 {@code audit_status} 字段（0=待审 / 1=通过 / 2=驳回），
 * 不驱动状态机流转，审核通过后由 {@code shelves} 完成状态机迁移。
 */
public final class SceneEvent {

    private SceneEvent() {
    }

    /** 状态机域标识（machine_code） */
    public static final String DOMAIN = "SCENE_SM";

    /** 上架：0→1 */
    public static final String SHELVES = "shelves";
    /** 下架：1→2 */
    public static final String OFFSHELVES = "offshelves";
    /** 重新上架：2→1 */
    public static final String RESHELVES = "reshelves";

    // ====== 状态码常量 ======

    /** 草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 已上架 */
    public static final int STATUS_PUBLISHED = 1;
    /** 已下架 */
    public static final int STATUS_OFFLINE = 2;
    /** 已满期 */
    public static final int STATUS_FULL = 3;

    // ====== 审核状态常量（audit_status 字段） ======

    /** 待审 */
    public static final int AUDIT_PENDING = 0;
    /** 审核通过 */
    public static final int AUDIT_PASS = 1;
    /** 审核驳回 */
    public static final int AUDIT_REJECT = 2;
}
