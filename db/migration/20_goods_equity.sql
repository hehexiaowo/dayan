-- ============================================================
-- 增量2：goods_equity + goods_service_item_rel
-- 权益商品配置表（1:1 goods_info）+ 商品-服务项目关联表（N:M）
-- ============================================================

-- ---------- goods_equity：权益商品配置（1:1 关联 goods_info） ----------
CREATE TABLE IF NOT EXISTS `goods_equity` (
  `id`               BIGINT AUTO_INCREMENT    COMMENT '主键',
  `goods_code`       VARCHAR(50)  NOT NULL    COMMENT '商品编码（1:1关联goods_info.goods_code）',
  `person_count`     INT          NOT NULL DEFAULT 1  COMMENT '使用人人数（1=个人版,2=双人版,3+家庭版）',
  `valid_days`       INT          NOT NULL DEFAULT 365 COMMENT '激活后有效天数',
  `shelf_life_days`  INT          NOT NULL DEFAULT 730 COMMENT '库存有效期天数（未激活时）',
  `max_transferable` TINYINT(1)   NOT NULL DEFAULT 1   COMMENT '是否可转让（0否1是）',
  `description`      TEXT         NULL        COMMENT '权益配置说明',
  `sort_order`       INT          NOT NULL DEFAULT 0,
  `status`           TINYINT(1)   NOT NULL DEFAULT 1    COMMENT '0=停用,1=启用',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator`          VARCHAR(64)  NOT NULL DEFAULT 'system',
  `updater`          VARCHAR(64)  NOT NULL DEFAULT 'system',
  `deleted`          TINYINT(1)   NOT NULL DEFAULT 0,
  `deleted_at`       DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_code` (`goods_code`, `deleted`)
) COMMENT='权益商品配置（商品类型特有配置，1:1关联goods_info）';

-- ---------- goods_service_item_rel：商品-服务项目关联（N:M） ----------
CREATE TABLE IF NOT EXISTS `goods_service_item_rel` (
  `id`          BIGINT AUTO_INCREMENT,
  `goods_code`  VARCHAR(50)  NOT NULL  COMMENT '商品编码（关联goods_equity.goods_code）',
  `item_code`   VARCHAR(50)  NOT NULL  COMMENT '服务项目编码（关联service_item.item_code）',
  `quantity`    INT          NOT NULL DEFAULT 1,
  `sort_order`  INT          NOT NULL DEFAULT 0,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator`     VARCHAR(64)  NOT NULL DEFAULT 'system',
  `updater`     VARCHAR(64)  NOT NULL DEFAULT 'system',
  `deleted`     TINYINT(1)   NOT NULL DEFAULT 0,
  `deleted_at`  DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_item` (`goods_code`, `item_code`, `deleted`),
  KEY `idx_goods_code` (`goods_code`),
  KEY `idx_item_code` (`item_code`)
) COMMENT='商品-服务项目关联表（一个权益商品组合N个服务项目）';
