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

-- ---------------------------------------------------------------------
-- 种子数据：3 个核心安排权益 service_item（按 subtype 区分）
--   SI00001 旅居(subtype=1) / SI00007 活力长居(subtype=2) / SI00005 照护长居(subtype=3)
-- ---------------------------------------------------------------------
INSERT INTO `service_item`
  (`item_code`, `item_name`, `item_category`, `item_subtype`, `item_value`,
   `cost_bearing`, `service_network`, `valid_days`, `max_use_count`,
   `description`, `sort_order`, `status`, `creator`, `updater`)
VALUES
  ('SI00001', '安排权益·旅居', 1, 1, 1000.00,
   0, '["*"]', 365, 10,
   '旅居权益安排（短期度假式养老），含优惠入住权、优先预订权', 0, 1, 'system', 'system'),
  ('SI00007', '安排权益·活力长居', 1, 2, 500.00,
   0, '["*"]', 365, 10,
   '活力长者长期入住安排权益（自理型），含优先入住权、优惠入住权', 0, 1, 'system', 'system'),
  ('SI00005', '安排权益·照护长居', 1, 3, 500.00,
   0, '["*"]', 365, 10,
   '照护型长期入住安排权益（失能/半失能），含保证入住权、优先入住权', 0, 1, 'system', 'system')
ON DUPLICATE KEY UPDATE `id` = `id`;

SET FOREIGN_KEY_CHECKS = 1;
