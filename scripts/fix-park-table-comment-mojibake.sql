-- =================================================================
-- fix-park-table-comment-mojibake.sql
-- 修复 park_asset / park_pricing / park_pricing_item / park_score
-- 四张表 COMMENT 的 cp1252 双重编码乱码。
-- 
-- 生成器: scripts/gen_fix_park_comment_mojibake.py
-- 修复原理: latin1(=cp1252) 双重编码逆向还原
-- 安全性: 仅改 COMMENT；列定义取自 information_schema 现状，不动其它属性。
--         执行前请先 review；本操作对正确 COMMENT 有破坏性，仅限这四张表。
-- =================================================================
SET NAMES utf8mb4;

-- park_asset
ALTER TABLE `park_asset`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  MODIFY COLUMN `park_code` varchar(64) NOT NULL COMMENT '机构编码',
  MODIFY COLUMN `asset_type` tinyint NOT NULL COMMENT '素材类型（1=图片 2=视频 3=文件 4=VR）',
  MODIFY COLUMN `asset_url` varchar(500) NOT NULL COMMENT '文件 OSS key（存 key 非完整 URL）',
  MODIFY COLUMN `asset_name` varchar(200) NULL DEFAULT NULL COMMENT '文件名称',
  MODIFY COLUMN `asset_category` tinyint NULL DEFAULT NULL COMMENT '业务分类（图片:1=外观..11=其他 视频:1=宣传..3=活动 文件:1=资质..5=其他 VR:1=全景..3=视频VR）',
  MODIFY COLUMN `description` varchar(500) NULL DEFAULT NULL COMMENT '描述',
  MODIFY COLUMN `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小（字节）',
  MODIFY COLUMN `width` int NULL DEFAULT NULL COMMENT '图片宽度px（图片专属）',
  MODIFY COLUMN `height` int NULL DEFAULT NULL COMMENT '图片高度px（图片专属）',
  MODIFY COLUMN `is_cover` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否封面（图片专属 0=否 1=是）',
  MODIFY COLUMN `cover_url` varchar(500) NULL DEFAULT NULL COMMENT '封面图key（视频专属）',
  MODIFY COLUMN `duration` int NULL DEFAULT NULL COMMENT '时长秒（视频专属）',
  MODIFY COLUMN `file_format` varchar(20) NULL DEFAULT NULL COMMENT '文件格式（文件专属 pdf/doc/xls等）',
  MODIFY COLUMN `vr_provider` varchar(100) NULL DEFAULT NULL COMMENT 'VR服务提供商（VR专属）',
  MODIFY COLUMN `thumbnail_url` varchar(500) NULL DEFAULT NULL COMMENT '缩略图key（VR专属）',
  MODIFY COLUMN `source_type` varchar(30) NOT NULL DEFAULT 'media_mgmt' COMMENT '来源（media_mgmt=素材库直传 room_type=房型 food_type=餐饮 facility=设施 service_item=服务 display_block=展示板块 adviser=顾问 park_info=机构信息）',
  MODIFY COLUMN `source_ref_code` varchar(64) NULL DEFAULT NULL COMMENT '来源编码（media_mgmt 时为 NULL）',
  MODIFY COLUMN `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  MODIFY COLUMN `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏 1=显示）',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  MODIFY COLUMN `creator` varchar(64) NULL DEFAULT 'system' COMMENT '创建人',
  MODIFY COLUMN `updater` varchar(64) NULL DEFAULT 'system' COMMENT '更新人',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  MODIFY COLUMN `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  COMMENT = '机构素材库（统一管理所有来源的图片/视频/文件/VR）';

-- park_pricing
ALTER TABLE `park_pricing`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  MODIFY COLUMN `park_code` varchar(64) NOT NULL COMMENT '机构编码',
  MODIFY COLUMN `plan_name` varchar(100) NULL DEFAULT NULL COMMENT '方案名称（如"豪华单人间·月费"）',
  MODIFY COLUMN `charge_type` tinyint NOT NULL COMMENT '费类（1=房间费 2=照护费 3=餐费 4=押金 5=设施费 6=服务费 9=其他）',
  MODIFY COLUMN `ref_type` varchar(20) NOT NULL COMMENT '关联类型（room_type/care_type/food_type/facility/service_item/park）',
  MODIFY COLUMN `ref_code` varchar(64) NOT NULL COMMENT '关联编码',
  MODIFY COLUMN `ref_name` varchar(100) NULL DEFAULT NULL COMMENT '关联名称（冗余）',
  MODIFY COLUMN `billing_cycle` tinyint NULL DEFAULT NULL COMMENT '计费周期（1=月 2=季 3=半年 4=年 5=一次性）',
  MODIFY COLUMN `price_unit` varchar(50) NULL DEFAULT NULL COMMENT '自由文本计费单位（设施/服务的 次/小时/场）',
  MODIFY COLUMN `original_price` decimal(12,2) NULL DEFAULT NULL COMMENT '原价',
  MODIFY COLUMN `sale_price` decimal(12,2) NOT NULL COMMENT '售价',
  MODIFY COLUMN `discount_rate` decimal(5,2) NULL DEFAULT NULL COMMENT '折扣率',
  MODIFY COLUMN `price_description` varchar(500) NULL DEFAULT NULL COMMENT '价格说明',
  MODIFY COLUMN `includes_items` text NULL DEFAULT NULL COMMENT '包含项目（JSON数组）',
  MODIFY COLUMN `effective_date` date NOT NULL COMMENT '生效日期',
  MODIFY COLUMN `expire_date` date NULL DEFAULT NULL COMMENT '失效日期（NULL=长期有效）',
  MODIFY COLUMN `is_current` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否当前生效价格（0=历史 1=当前）',
  MODIFY COLUMN `is_promotion` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否促销价',
  MODIFY COLUMN `promotion_description` varchar(200) NULL DEFAULT NULL COMMENT '促销说明',
  MODIFY COLUMN `price_change_reason` varchar(500) NULL DEFAULT NULL COMMENT '价格变更原因',
  MODIFY COLUMN `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  MODIFY COLUMN `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停用 1=启用）',
  MODIFY COLUMN `version` bigint NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  MODIFY COLUMN `creator` varchar(64) NULL DEFAULT 'system' COMMENT '创建人',
  MODIFY COLUMN `updater` varchar(64) NULL DEFAULT 'system' COMMENT '更新人',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  MODIFY COLUMN `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  MODIFY COLUMN `current_key` VARCHAR(200) GENERATED ALWAYS AS
      (CASE WHEN `is_current` = 1 AND `deleted` = 0
       THEN CONCAT(`park_code`, '|', `charge_type`, '|', `ref_code`, '|',
                   COALESCE(CAST(`billing_cycle` AS CHAR), '0'))
       ELSE NULL END) STORED
  COMMENT '当前价唯一键（生成列）',
  COMMENT = '机构统一定价方案';

-- park_pricing_item
ALTER TABLE `park_pricing_item`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  MODIFY COLUMN `pricing_id` bigint NOT NULL COMMENT 'FK→park_pricing.id',
  MODIFY COLUMN `park_code` varchar(64) NOT NULL COMMENT '机构编码',
  MODIFY COLUMN `item_type` varchar(20) NOT NULL COMMENT '关联类型（room_type/care_type/food_type/facility/service_item）',
  MODIFY COLUMN `item_code` varchar(64) NOT NULL COMMENT '关联编码',
  MODIFY COLUMN `item_name` varchar(100) NULL DEFAULT NULL COMMENT '关联名称（冗余）',
  MODIFY COLUMN `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  MODIFY COLUMN `creator` varchar(64) NULL DEFAULT 'system' COMMENT '创建人',
  MODIFY COLUMN `updater` varchar(64) NULL DEFAULT 'system' COMMENT '更新人',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  MODIFY COLUMN `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  COMMENT = '机构定价明细行（套餐关联）';

-- park_score
ALTER TABLE `park_score`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  MODIFY COLUMN `park_code` varchar(64) NOT NULL COMMENT '机构编码',
  MODIFY COLUMN `score_total` int NULL DEFAULT NULL COMMENT '总评分',
  MODIFY COLUMN `score_environment` int NULL DEFAULT NULL COMMENT '环境评分',
  MODIFY COLUMN `score_recreation` int NULL DEFAULT NULL COMMENT '文娱评分',
  MODIFY COLUMN `score_nursing` int NULL DEFAULT NULL COMMENT '医养护理评分',
  MODIFY COLUMN `score_food` int NULL DEFAULT NULL COMMENT '餐食精细评分',
  MODIFY COLUMN `score_service` int NULL DEFAULT NULL COMMENT '服务品质评分',
  MODIFY COLUMN `score_price` int NULL DEFAULT NULL COMMENT '价格评分',
  MODIFY COLUMN `score_description` varchar(255) NULL DEFAULT NULL COMMENT '评分描述',
  MODIFY COLUMN `version` bigint NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  MODIFY COLUMN `creator` varchar(64) NULL DEFAULT 'system' COMMENT '创建人',
  MODIFY COLUMN `updater` varchar(64) NULL DEFAULT 'system' COMMENT '更新人',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  MODIFY COLUMN `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  COMMENT = '机构评分（独立表）';

