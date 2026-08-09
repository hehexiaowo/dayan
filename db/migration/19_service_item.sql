-- 19_service_item.sql
-- 服务项目表（权益模板重构：原子服务能力，安排权益/费用权益两大类）

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `service_item` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `item_code`       VARCHAR(50)  NOT NULL                 COMMENT '服务项目编码（SI+5位）',
  `item_name`       VARCHAR(200) NOT NULL                 COMMENT '服务项目名称',
  `item_category`   TINYINT(2)   NOT NULL                 COMMENT '项目大类（1=安排权益，2=费用权益）',
  `item_subtype`    TINYINT(2)   NULL                     COMMENT '安排权益子类（1=旅居,2=活力长居,3=照护长居）；费用权益为NULL',
  `item_value`      DECIMAL(12,2) NULL                    COMMENT '面值/单价（元）。安排权益=经纪服务费面值；费用权益=单位补贴额度',
  `cost_bearing`    TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '费用承担方（0=客户自负，1=系统承担）',
  `service_network` TEXT         NULL                     COMMENT '服务网络JSON数组：元素为park_code或通配模式(如"旅居*"、"*"、精确码)',
  `covered_items`   TEXT         NULL                     COMMENT '费用权益补贴明细JSON：[{room_type,service_content,quantity}]；安排权益为NULL',
  `valid_days`      INT          NOT NULL DEFAULT 365     COMMENT '激活后有效天数',
  `max_use_count`   INT          NOT NULL DEFAULT 1       COMMENT '最大使用次数',
  `description`     TEXT         NULL                     COMMENT '服务项目说明',
  `sort_order`      INT          NOT NULL DEFAULT 0       COMMENT '排序号',
  `status`          TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '状态（0=停用,1=启用）',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator`         VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
  `updater`         VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
  `deleted`         TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '逻辑删除（0=未删,1=已删）',
  `deleted_at`      DATETIME     NULL                     COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_code` (`item_code`),
  KEY `idx_item_category` (`item_category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务项目（原子服务能力：安排权益/费用权益两大类）';

SET FOREIGN_KEY_CHECKS = 1;
