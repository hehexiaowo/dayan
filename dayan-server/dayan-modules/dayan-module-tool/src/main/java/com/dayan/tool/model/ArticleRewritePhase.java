package com.dayan.tool.model;

/**
 * 文章转写阶段状态常量。
 *
 * <p>状态流转：CREATED → CONTENT_FETCHED → SUMMARY_DONE → PLANNED → REWRITTEN → AUDITED → IMAGED → READY → PUBLISHED
 */
public final class ArticleRewritePhase {

    /** 已创建（初始状态） */
    public static final String CREATED = "CREATED";

    /** 内容已获取（第一步完成） */
    public static final String CONTENT_FETCHED = "CONTENT_FETCHED";

    /** 总结与判断完成，转写方案已生成（待确认） */
    public static final String SUMMARY_DONE = "SUMMARY_DONE";

    /** 转写方案已确认（待转写） */
    public static final String PLANNED = "PLANNED";

    /** 转写完成（第四步完成） */
    public static final String REWRITTEN = "REWRITTEN";

    /** 审核完成（第四步完成） */
    public static final String AUDITED = "AUDITED";

    /** 配图完成（第五步完成） */
    public static final String IMAGED = "IMAGED";

    /** 待发布（第六步自查通过） */
    public static final String READY = "READY";

    /** 已发布 */
    public static final String PUBLISHED = "PUBLISHED";

    /** 已保存到内容库 */
    public static final String SAVED = "SAVED";

    private ArticleRewritePhase() {}
}
