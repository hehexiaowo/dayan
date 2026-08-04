-- =====================================================================
-- 03_butler.sql  养老管家域（butler_）
-- 域说明：养老管家信息、账号、排班、客户关系、服务记录、评价（简化版RBAC）
-- 表数：8
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.3
-- 主键策略：butler_info/butler_account/butler_account_role_rel/butler_skill 为平台共享表（AUTO_INCREMENT）；
--           butler_schedule/butler_client_rel/butler_service_record/butler_rating 为分片表（雪花ID）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.3.1 butler_info 养老管家信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `butler_info`;
CREATE TABLE `butler_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码（BL+5位数字）',
  `full_name` VARCHAR(50) NOT NULL COMMENT '管家姓名',
  `phone` VARCHAR(32) NOT NULL COMMENT '手机号',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '所属组织编码',
  `butler_level` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '管家等级（1=初级, 2=中级, 3=高级, 4=金牌）',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=离职, 1=在职）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_butler_code` (`butler_code`),
  KEY `idx_organ_code` (`organ_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养老管家信息';

-- ---------------------------------------------------------------------
-- 3.3.2 butler_account 养老管家账号
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `butler_account`;
CREATE TABLE `butler_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号（支持手机号登录）',
  `password` VARCHAR(200) NOT NULL COMMENT '密码（加密存储）',
  `salt` VARCHAR(50) DEFAULT NULL COMMENT '密码盐值',
  `open_id` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID（支持微信登录）',
  `union_id` VARCHAR(100) DEFAULT NULL COMMENT '微信UnionID（支持微信登录）',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `account_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '账号状态（0=锁定, 1=正常, 2=禁用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养老管家账号';

-- ---------------------------------------------------------------------
-- 3.3.3 butler_schedule 管家排班（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `butler_schedule`;
CREATE TABLE `butler_schedule` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码',
  `schedule_date` DATE NOT NULL COMMENT '排班日期',
  `schedule_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '排班类型（1=工作日, 2=休息日, 3=请假）',
  `start_time` TIME DEFAULT NULL COMMENT '上班时间',
  `end_time` TIME DEFAULT NULL COMMENT '下班时间',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=无效, 1=有效）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_butler_date` (`butler_code`, `schedule_date`),
  KEY `idx_schedule_date` (`schedule_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管家排班';

-- ---------------------------------------------------------------------
-- 3.3.4 butler_client_rel 管家-客户关系（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `butler_client_rel`;
CREATE TABLE `butler_client_rel` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `bind_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=已解绑, 1=服务中）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_butler_client` (`butler_code`, `client_code`, `status`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_client_code` (`client_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管家-客户关系';

-- ---------------------------------------------------------------------
-- 3.3.5 butler_service_record 管家服务记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `butler_service_record`;
CREATE TABLE `butler_service_record` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `service_type` TINYINT(2) NOT NULL COMMENT '服务类型（1=需求评估, 2=方案定制, 3=全程安排, 4=回访品控）',
  `service_title` VARCHAR(200) DEFAULT NULL COMMENT '服务标题',
  `service_date` DATE NOT NULL COMMENT '服务日期',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=进行中, 1=已完成, 2=已取消）',
  `communicate_way` TINYINT(2) DEFAULT NULL COMMENT '沟通方式（1=电话, 2=企业微信, 3=微信, 4=当面沟通, 5=其他）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_service_date` (`service_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管家服务记录';

-- ---------------------------------------------------------------------
-- 3.3.6 butler_rating 管家评价（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `butler_rating`;
CREATE TABLE `butler_rating` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `service_record_code` VARCHAR(64) DEFAULT NULL COMMENT '关联服务记录编码',
  `rating` TINYINT(1) NOT NULL COMMENT '评分（1-5）',
  `content` TEXT DEFAULT NULL COMMENT '评价内容',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=已隐藏, 1=正常）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管家评价';

-- ---------------------------------------------------------------------
-- 3.3.7 butler_account_role_rel 管家账号角色关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `butler_account_role_rel`;
CREATE TABLE `butler_account_role_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account_code` VARCHAR(64) NOT NULL COMMENT '管家账号编码',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码',
  `role_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '角色类型（1=普通管家, 2=高级管家, 3=管家主管）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_role` (`account_code`, `role_type`),
  KEY `idx_butler_code` (`butler_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管家账号角色关联';

-- ---------------------------------------------------------------------
-- 3.3.8 butler_skill 管家技能标签
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `butler_skill`;
CREATE TABLE `butler_skill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码',
  `skill_code` VARCHAR(50) NOT NULL COMMENT '技能编码（字典：butler_skill）',
  `skill_name` VARCHAR(100) NOT NULL COMMENT '技能名称',
  `proficiency` TINYINT(2) DEFAULT NULL COMMENT '熟练度（1=了解, 2=熟悉, 3=熟练, 4=精通）',
  `is_certified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否持证（0=否, 1=是）',
  `certificate_no` VARCHAR(100) DEFAULT NULL COMMENT '证书编号',
  `obtain_date` DATE DEFAULT NULL COMMENT '取得日期',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_butler_skill` (`butler_code`, `skill_code`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_skill_code` (`skill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管家技能标签';
