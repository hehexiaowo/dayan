-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 62_goods_display_block.sql  商品页面配置（展示板块 + banner/缩略图）
--
-- 对齐 park 域先例（park_display_block 独立表 + park_info.*_config 内联 JSON）：
-- 1. goods_display_block：结构化页面板块（每条 = 一个 tab 的结构化介绍），
--    类型+标题+富文本+多图（带描述）+排序+状态；C/Agent 端详情页按排序渲染为 tab。
-- 2. goods_info.display_config：轻量展示配置内联 JSON {"banners":[key...],"thumbnail":"key"}，
--    banner 轮播图与列表缩略图（量少、随详情一次读出、无独立管理需求，不建表）；
--    thumbnail 空 = 默认第一张 / 回退 cover_image。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `goods_display_block` (
  `id`                 BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_code`         VARCHAR(50) NOT NULL COMMENT '商品编码（关联goods_info.goods_code）',
  `block_type`         VARCHAR(50) NOT NULL COMMENT '板块类型（product_intro=产品介绍, rights_detail=权益详解, service_flow=服务流程, faq=常见问题, purchase_terms=购买须知, custom=自定义）',
  `block_title`        VARCHAR(100) DEFAULT NULL COMMENT '板块标题（C端 tab 名，如"权益详解"）',
  `content`            TEXT DEFAULT NULL COMMENT '富文本内容（HTML）',
  `images`             TEXT DEFAULT NULL COMMENT '图片key列表（JSON数组）',
  `image_descriptions` TEXT DEFAULT NULL COMMENT '图片描述列表（JSON数组，与images一一对应）',
  `sort_order`         INT NOT NULL DEFAULT 0 COMMENT '排序号（tab 顺序）',
  `status`             TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏, 1=显示）',
  `created_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator`            VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `updater`            VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  `deleted`            TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删,1=已删）',
  `deleted_at`         DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_goods_code` (`goods_code`),
  KEY `idx_goods_type` (`goods_code`, `block_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品展示内容板块（C/Agent端详情页 tab）';

ALTER TABLE `goods_info`
  ADD COLUMN `display_config` TEXT NULL
    COMMENT '页面展示配置JSON：{"banners":["key"],"thumbnail":"key"}；thumbnail空=默认第一张/回退cover_image' AFTER `cover_image`;
