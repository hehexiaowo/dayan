-- =====================================================================
-- 09_client.sql  客户域（client_）
-- 域说明：客户信息、账号、收藏、健康档案、照护需求、家庭成员、收货地址（按渠道隔离）
-- 表数：7
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.9
-- 主键策略：全部为分片表（雪花ID），含 channel_code，参与渠道分片
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.9.1 client_info 客户信息（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `client_info`;
CREATE TABLE `client_info` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码（CL+5位数字，渠道内唯一）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '所属渠道编码（按渠道隔离）',
  `full_name` VARCHAR(50) NOT NULL COMMENT '客户姓名',
  `gender` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '性别（0=未知, 1=男, 2=女）',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `birthday` DATE DEFAULT NULL COMMENT '出生日期',
  `age` TINYINT(3) DEFAULT NULL COMMENT '年龄（冗余，定期更新）',
  `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号（加密存储）',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `province_code` VARCHAR(20) DEFAULT NULL COMMENT '省份编码',
  `city_code` VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
  `district_code` VARCHAR(20) DEFAULT NULL COMMENT '区划编码',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `nationality` VARCHAR(50) DEFAULT NULL COMMENT '国籍',
  `ethnic` VARCHAR(50) DEFAULT NULL COMMENT '民族',
  `education` TINYINT(2) DEFAULT NULL COMMENT '学历',
  `marital_status` TINYINT(2) DEFAULT NULL COMMENT '婚姻状况（1=未婚, 2=已婚, 3=离异, 4=丧偶）',
  `profession` VARCHAR(100) DEFAULT NULL COMMENT '职业',
  `source_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '来源渠道（1=自主注册, 2=代理人邀请, 3=权益激活, 4=管家录入）',
  `source_agent_code` VARCHAR(64) DEFAULT NULL COMMENT '来源代理人编码',
  `source_channel_code` VARCHAR(50) DEFAULT NULL COMMENT '来源渠道编码',
  `client_level` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '客户等级（1=普通, 2=VIP, 3=SVIP）',
  `equity_count` INT(11) NOT NULL DEFAULT 0 COMMENT '持有权益数',
  `used_equity_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已使用权益数',
  `service_count` INT(11) NOT NULL DEFAULT 0 COMMENT '累计服务次数',
  `total_order_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '累计消费金额',
  `last_service_time` DATETIME DEFAULT NULL COMMENT '最近服务时间',
  `register_time` DATETIME DEFAULT NULL COMMENT '注册时间',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `is_vip` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否VIP（0=否, 1=是）',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=正常, 2=冻结）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_client_code` (`channel_code`, `client_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_id_card` (`id_card`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_source_agent_code` (`source_agent_code`),
  KEY `idx_client_level` (`client_level`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户信息';

-- ---------------------------------------------------------------------
-- 3.9.2 client_account 客户账号（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `client_account`;
CREATE TABLE `client_account` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码（渠道内唯一，与本渠道 client_info 1:1）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '所属渠道编码（登录隔离维度）',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '登录用户名（渠道内唯一）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '登录手机号（不要求全局唯一）',
  `password` VARCHAR(200) DEFAULT NULL COMMENT '密码（BCrypt 加密存储）',
  `salt` VARCHAR(50) DEFAULT NULL COMMENT '密码盐值',
  `open_id` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID（不要求全局唯一）',
  `union_id` VARCHAR(100) DEFAULT NULL COMMENT '微信UnionID',
  `alipay_id` VARCHAR(100) DEFAULT NULL COMMENT '支付宝账号ID',
  `ext_account_no` VARCHAR(100) DEFAULT NULL COMMENT '渠道本身账号系统唯一编码（NULL=大雁自建账号）',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_count` INT(11) NOT NULL DEFAULT 0 COMMENT '累计登录次数',
  `account_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '账号状态（0=锁定, 1=正常, 2=禁用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_username` (`channel_code`, `username`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`),
  KEY `idx_ext_account_no` (`ext_account_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户账号';

-- ---------------------------------------------------------------------
-- 3.9.3 client_favorite 客户收藏夹（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `client_favorite`;
CREATE TABLE `client_favorite` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `target_type` TINYINT(2) NOT NULL COMMENT '收藏对象类型（1=养老机构, 2=场景, 3=课程, 4=内容）',
  `target_code` VARCHAR(50) NOT NULL COMMENT '收藏对象编码',
  `target_name` VARCHAR(200) DEFAULT NULL COMMENT '收藏对象名称',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_target` (`client_code`, `target_type`, `target_code`),
  KEY `idx_client_code` (`client_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户收藏夹';

-- ---------------------------------------------------------------------
-- 3.9.4 client_health_profile 客户健康档案（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `client_health_profile`;
CREATE TABLE `client_health_profile` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `height` DECIMAL(5,1) DEFAULT NULL COMMENT '身高（cm）',
  `weight` DECIMAL(5,1) DEFAULT NULL COMMENT '体重（kg）',
  `blood_type` TINYINT(2) DEFAULT NULL COMMENT '血型（1=A, 2=B, 3=AB, 4=O）',
  `blood_pressure` VARCHAR(50) DEFAULT NULL COMMENT '血压（如120/80）',
  `blood_sugar` DECIMAL(5,2) DEFAULT NULL COMMENT '血糖（mmol/L）',
  `heart_rate` INT(11) DEFAULT NULL COMMENT '心率（次/分）',
  `chronic_diseases` TEXT DEFAULT NULL COMMENT '慢性病列表（JSON数组）',
  `allergy_history` TEXT DEFAULT NULL COMMENT '过敏史',
  `surgery_history` TEXT DEFAULT NULL COMMENT '手术史',
  `family_history` TEXT DEFAULT NULL COMMENT '家族病史',
  `medication_info` TEXT DEFAULT NULL COMMENT '当前用药信息',
  `mobility_level` TINYINT(2) DEFAULT NULL COMMENT '行动能力（1=完全自理, 2=部分自理, 3=需要协助, 4=完全依赖）',
  `cognitive_level` TINYINT(2) DEFAULT NULL COMMENT '认知能力（1=正常, 2=轻度障碍, 3=中度障碍, 4=重度障碍）',
  `mental_status` TINYINT(2) DEFAULT NULL COMMENT '心理状态（1=良好, 2=一般, 3=需关注）',
  `diet_preference` TEXT DEFAULT NULL COMMENT '饮食偏好（JSON数组）',
  `sleep_quality` TINYINT(2) DEFAULT NULL COMMENT '睡眠质量（1=良好, 2=一般, 3=较差）',
  `emergency_contact_name` VARCHAR(50) DEFAULT NULL COMMENT '紧急联系人姓名',
  `emergency_contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '紧急联系人电话',
  `emergency_contact_relation` VARCHAR(20) DEFAULT NULL COMMENT '紧急联系人关系',
  `health_score` DECIMAL(5,2) DEFAULT NULL COMMENT '健康评分',
  `last_assessment_time` DATETIME DEFAULT NULL COMMENT '最近评估时间',
  `remark` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_mobility_level` (`mobility_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户健康档案';

-- ---------------------------------------------------------------------
-- 3.9.5 client_care_need 客户照护需求评估（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `client_care_need`;
CREATE TABLE `client_care_need` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `butler_code` VARCHAR(64) DEFAULT NULL COMMENT '评估管家编码',
  `butler_full_name` VARCHAR(50) DEFAULT NULL COMMENT '评估管家姓名（快照）',
  `eval_date` DATE NOT NULL COMMENT '评估日期',
  `care_level` TINYINT(2) DEFAULT NULL COMMENT '建议照护等级（1=特级, 2=一级, 3=二级, 4=三级, 5=自理）',
  `care_type_preference` VARCHAR(200) DEFAULT NULL COMMENT '偏好照护类型（JSON数组）',
  `living_preference` VARCHAR(200) DEFAULT NULL COMMENT '居住偏好（JSON数组）',
  `food_preference` VARCHAR(200) DEFAULT NULL COMMENT '饮食偏好',
  `budget_min` DECIMAL(12,2) DEFAULT NULL COMMENT '预算下限（元/月）',
  `budget_max` DECIMAL(12,2) DEFAULT NULL COMMENT '预算上限（元/月）',
  `area_preference` VARCHAR(200) DEFAULT NULL COMMENT '区域偏好（JSON数组）',
  `special_requirements` TEXT DEFAULT NULL COMMENT '特殊需求说明',
  `expected_checkin_date` DATE DEFAULT NULL COMMENT '期望入住日期',
  `park_recommendations` TEXT DEFAULT NULL COMMENT '推荐机构列表（JSON数组）',
  `eval_result` TEXT DEFAULT NULL COMMENT '评估结论',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=评估中, 1=已完成, 2=已过期）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_eval_date` (`eval_date`),
  KEY `idx_care_level` (`care_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户照护需求评估';

-- ---------------------------------------------------------------------
-- 3.9.6 client_family_member 客户家庭成员/紧急联系人（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `client_family_member`;
CREATE TABLE `client_family_member` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `member_name` VARCHAR(50) NOT NULL COMMENT '成员姓名',
  `relation` VARCHAR(20) NOT NULL COMMENT '与客户关系（子女/配偶/兄弟姐妹等）',
  `gender` TINYINT(1) DEFAULT 0 COMMENT '性别（0=未知, 1=男, 2=女）',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `is_emergency_contact` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否紧急联系人（0=否, 1=是）',
  `is_primary_contact` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主要联系人（0=否, 1=是）',
  `is_decision_maker` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否决策人（0=否, 1=是）',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '地址',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_is_emergency_contact` (`is_emergency_contact`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户家庭成员/紧急联系人';

-- ---------------------------------------------------------------------
-- 3.9.7 client_address 客户收货地址（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `client_address`;
CREATE TABLE `client_address` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province_code` VARCHAR(20) DEFAULT NULL COMMENT '省编码',
  `city_code` VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
  `district_code` VARCHAR(20) DEFAULT NULL COMMENT '区编码',
  `detail_address` VARCHAR(256) NOT NULL COMMENT '详细地址',
  `full_address` VARCHAR(500) DEFAULT NULL COMMENT '完整地址（省市区+详细）',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认地址（0=否, 1=是）',
  `tag` VARCHAR(32) DEFAULT NULL COMMENT '地址标签（家/公司等）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户收货地址';
