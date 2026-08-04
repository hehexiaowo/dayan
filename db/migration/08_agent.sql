-- =====================================================================
-- 08_agent.sql  代理人域（agent_）
-- 域说明：保险代理人信息、账号、收藏、客户绑定、业绩、分享记录（按渠道隔离）
-- 表数：6
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.8
-- 主键策略：全部为分片表（雪花ID），含 channel_code，参与渠道分片
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.8.1 agent_info 代理人信息（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `agent_info`;
CREATE TABLE `agent_info` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '代理人编码（AG+5位数字，渠道内唯一）',
  `full_name` VARCHAR(50) NOT NULL COMMENT '代理人姓名',
  `gender` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '性别（0=未知, 1=男, 2=女）',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号（加密存储）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '所属渠道编码',
  `company_name` VARCHAR(200) DEFAULT NULL COMMENT '保险公司名称',
  `branch_name` VARCHAR(200) DEFAULT NULL COMMENT '分支机构',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '部门',
  `position` VARCHAR(100) DEFAULT NULL COMMENT '职位',
  `employee_no` VARCHAR(50) DEFAULT NULL COMMENT '保险公司工号',
  `license_no` VARCHAR(50) DEFAULT NULL COMMENT '从业资格证号',
  `province_code` VARCHAR(20) DEFAULT NULL COMMENT '省份编码',
  `city_code` VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
  `district_code` VARCHAR(20) DEFAULT NULL COMMENT '区划编码',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `service_intro` TEXT DEFAULT NULL COMMENT '服务介绍',
  `client_count` INT(11) NOT NULL DEFAULT 0 COMMENT '服务客户数',
  `total_order_count` INT(11) NOT NULL DEFAULT 0 COMMENT '累计订单数',
  `total_order_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '累计订单金额',
  `agent_level` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '等级（1=普通, 2=银牌, 3=金牌, 4=钻石）',
  `is_certified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否认证（0=否, 1=是）',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=正常, 2=冻结）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_agent_code` (`channel_code`, `agent_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_agent_level` (`agent_level`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理人信息';

-- ---------------------------------------------------------------------
-- 3.8.2 agent_account 代理人账号（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `agent_account`;
CREATE TABLE `agent_account` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '代理人编码（渠道内唯一，与本渠道 agent_info 1:1）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '所属渠道编码（账号的渠道归属，登录隔离维度）',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '登录用户名（渠道内唯一，手机号/微信/ext 账号登录时可空）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '登录手机号（不要求全局唯一）',
  `password` VARCHAR(200) DEFAULT NULL COMMENT '密码（加密存储；渠道外部账号登录时可空）',
  `salt` VARCHAR(50) DEFAULT NULL COMMENT '密码盐值',
  `open_id` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID（不要求全局唯一）',
  `union_id` VARCHAR(100) DEFAULT NULL COMMENT '微信UnionID（不要求全局唯一）',
  `ext_account_no` VARCHAR(100) DEFAULT NULL COMMENT '渠道本身账号系统的唯一编码；NULL=大雁自建账号',
  `account_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '账号状态（0=锁定, 1=正常, 2=禁用）',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_username` (`channel_code`, `username`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`),
  KEY `idx_ext_account_no` (`ext_account_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理人账号';

-- ---------------------------------------------------------------------
-- 3.8.3 agent_favorite 代理人收藏夹（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `agent_favorite`;
CREATE TABLE `agent_favorite` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '代理人编码',
  `target_type` TINYINT(2) NOT NULL COMMENT '收藏对象类型（1=养老机构, 2=场景, 3=课程, 4=内容）',
  `target_code` VARCHAR(50) NOT NULL COMMENT '收藏对象编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_target` (`agent_code`, `target_type`, `target_code`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_target` (`target_type`, `target_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理人收藏夹';

-- ---------------------------------------------------------------------
-- 3.8.4 agent_client_rel 代理人-客户绑定关系（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `agent_client_rel`;
CREATE TABLE `agent_client_rel` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '代理人编码',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `bind_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '绑定类型（1=权益赠送绑定, 2=活动邀请绑定, 3=自主绑定）',
  `bind_time` DATETIME NOT NULL COMMENT '绑定时间',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=已解绑, 1=服务中）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理人-客户绑定关系';

-- ---------------------------------------------------------------------
-- 3.8.5 agent_performance 代理人业绩（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `agent_performance`;
CREATE TABLE `agent_performance` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '代理人编码',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '所属渠道编码',
  `period_type` TINYINT(2) NOT NULL COMMENT '统计周期（1=日, 2=周, 3=月, 4=季, 5=年）',
  `period_value` VARCHAR(20) NOT NULL COMMENT '周期值',
  `equity_grant_count` INT(11) NOT NULL DEFAULT 0 COMMENT '权益赠送次数',
  `equity_grant_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '权益赠送金额',
  `scene_order_count` INT(11) NOT NULL DEFAULT 0 COMMENT '场景订单数',
  `scene_order_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '场景订单金额',
  `course_order_count` INT(11) NOT NULL DEFAULT 0 COMMENT '课程订单数',
  `course_order_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '课程订单金额',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_period` (`agent_code`, `period_type`, `period_value`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理人业绩';

-- ---------------------------------------------------------------------
-- 3.8.6 agent_share_record 代理人分享记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `agent_share_record`;
CREATE TABLE `agent_share_record` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `share_code` VARCHAR(64) NOT NULL COMMENT '分享编码',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '代理人编码',
  `share_type` TINYINT(2) NOT NULL COMMENT '分享类型（1=内容, 2=场景, 3=机构, 4=权益, 5=课程）',
  `biz_code` VARCHAR(64) NOT NULL COMMENT '分享对象编码',
  `share_channel` TINYINT(2) DEFAULT NULL COMMENT '分享渠道（1=微信, 2=朋友圈, 3=复制链接, 4=二维码, 5=短信）',
  `client_code` VARCHAR(50) DEFAULT NULL COMMENT '接收客户编码（已知客户时）',
  `view_count` INT(11) NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `share_time` DATETIME NOT NULL COMMENT '分享时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_share_type` (`share_type`),
  KEY `idx_share_time` (`share_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理人分享记录';
