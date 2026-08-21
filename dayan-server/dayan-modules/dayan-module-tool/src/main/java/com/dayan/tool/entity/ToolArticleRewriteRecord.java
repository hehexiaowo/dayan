package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI文章转写记录。
 *
 * <p>按阶段组织JSON字段，每个阶段独立存储：
 * <ul>
 *   <li>contentFetch - 第一步：内容获取</li>
 *   <li>summaryAnalysis - 第二步：内容总结与价值判断</li>
 *   <li>rewriteResult - 第三步：文章转写</li>
 *   <li>auditResult - 第四步：内容审核</li>
 *   <li>imageResult - 第五步：文章配图</li>
 *   <li>publishInfo - 第六步：自查与发布</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_article_rewrite_record")
public class ToolArticleRewriteRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属工具实例（tool_info.tool_code） */
    private String toolCode;

    /** 创建代理人编码（登录上下文注入，防越权） */
    private String agentCode;

    /** 渠道编码（租户隔离） */
    private String channelCode;

    /** 状态（见 ArticleRewritePhase 常量） */
    private String status;

    /** 第一步：内容获取结果JSON */
    private String contentFetch;

    /** 第二步：总结与价值判断JSON */
    private String summaryAnalysis;

    /** 第三步：转写结果JSON */
    private String rewriteResult;

    /** 第四步：审核结果JSON */
    private String auditResult;

    /** 第五步：配图结果JSON */
    private String imageResult;

    /** 第六步：自查与发布信息JSON */
    private String publishInfo;
}
