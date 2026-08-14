-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 04_supplier.sql  供应商域（supplier_）
-- 域说明：养老机构运营方信息、资质、合同、评价、RBAC五表、开放平台对接
-- 表数：10
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.4
-- 主键策略：全部为平台共享表（AUTO_INCREMENT）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.4.1 supplier_info 供应商信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_info`;
CREATE TABLE `supplier_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码（SP+5位数字）',
  `full_name` VARCHAR(200) NOT NULL COMMENT '供应商全称',
  `short_name` VARCHAR(50) DEFAULT NULL COMMENT '简称',
  `supplier_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '供应商类型（1=养老机构, 2=服务提供商, 3=设备供应商）',
  `unified_credit_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
  `legal_person` VARCHAR(50) DEFAULT NULL COMMENT '法定代表人',
  `registered_capital` DECIMAL(12,2) DEFAULT NULL COMMENT '注册资本（万元）',
  `establish_date` DATE DEFAULT NULL COMMENT '成立日期',
  `business_license_no` VARCHAR(100) DEFAULT NULL COMMENT '营业执照编号',
  `business_scope` TEXT DEFAULT NULL COMMENT '经营范围',
  `province_code` VARCHAR(20) DEFAULT NULL COMMENT '省份编码',
  `city_code` VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
  `district_code` VARCHAR(20) DEFAULT NULL COMMENT '区划编码',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `contact_email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  `logo_url` VARCHAR(500) DEFAULT NULL COMMENT 'Logo图片URL',
  `description` TEXT DEFAULT NULL COMMENT '供应商介绍',
  `license_image` VARCHAR(500) DEFAULT NULL COMMENT '营业执照图片URL',
  `qualification_image` VARCHAR(500) DEFAULT NULL COMMENT '资质证书图片URL',
  `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号（加密存储）',
  `bank_account_name` VARCHAR(100) DEFAULT NULL COMMENT '银行户名',
  `park_count` INT(11) NOT NULL DEFAULT 0 COMMENT '下属机构数量',
  `cooperation_start_date` DATE DEFAULT NULL COMMENT '合作开始日期',
  `cooperation_end_date` DATE DEFAULT NULL COMMENT '合作结束日期',
  `commission_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '默认佣金比例',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待审核, 1=已合作, 2=已暂停, 3=已终止）',
  `audit_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '审核状态（0=待审核, 1=审核通过, 2=审核驳回）',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`),
  KEY `idx_full_name` (`full_name`(50)),
  KEY `idx_unified_credit_code` (`unified_credit_code`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商信息';

-- ---------------------------------------------------------------------
-- 3.4.2 supplier_open_platform 供应商开放平台
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_open_platform`;
CREATE TABLE `supplier_open_platform` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码',
  `platform_name` VARCHAR(100) NOT NULL COMMENT '平台名称',
  `api_base_url` VARCHAR(500) DEFAULT NULL COMMENT 'API基础地址',
  `app_key` VARCHAR(100) DEFAULT NULL COMMENT '应用Key',
  `app_secret` VARCHAR(200) DEFAULT NULL COMMENT '应用密钥（加密存储）',
  `callback_url` VARCHAR(500) DEFAULT NULL COMMENT '回调地址',
  `webhook_secret` VARCHAR(200) DEFAULT NULL COMMENT 'Webhook密钥（加密存储）',
  `protocol_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '协议类型（1=REST, 2=SOAP, 3=SDK）',
  `auth_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '认证方式（1=Token, 2=签名, 3=OAuth）',
  `data_format` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '数据格式（1=JSON, 2=XML）',
  `api_version` VARCHAR(20) NOT NULL DEFAULT 'v1' COMMENT 'API版本',
  `rate_limit` INT(11) DEFAULT 1000 COMMENT '调用频率限制（次/分钟）',
  `timeout` INT(11) DEFAULT 30 COMMENT '超时时间（秒）',
  `retry_count` INT(11) DEFAULT 3 COMMENT '重试次数',
  `extra_config` TEXT DEFAULT NULL COMMENT '扩展配置（JSON格式）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`),
  UNIQUE KEY `uk_app_key` (`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商开放平台';

-- ---------------------------------------------------------------------
-- 3.4.3 supplier_account 供应商账号
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_account`;
CREATE TABLE `supplier_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码',
  `account_code` VARCHAR(50) NOT NULL COMMENT '账号编码',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
  `password` VARCHAR(200) NOT NULL COMMENT '密码（加密存储）',
  `salt` VARCHAR(50) NOT NULL COMMENT '密码盐值',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `open_id` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID（支持微信登录）',
  `union_id` VARCHAR(100) DEFAULT NULL COMMENT '微信UnionID（支持微信登录）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `position` VARCHAR(100) DEFAULT NULL COMMENT '职位',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_count` INT(11) NOT NULL DEFAULT 0 COMMENT '累计登录次数',
  `pwd_update_time` DATETIME DEFAULT NULL COMMENT '密码修改时间',
  `account_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '账号状态（0=锁定, 1=正常, 2=禁用）',
  `is_admin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否管理员账号（0=否, 1=是）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_code` (`account_code`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_supplier_code` (`supplier_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商账号';

-- ---------------------------------------------------------------------
-- 3.4.4 supplier_role 供应商角色
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_role`;
CREATE TABLE `supplier_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '所属供应商编码',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `role_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '角色类型（1=系统预置, 2=自定义）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商角色';

-- ---------------------------------------------------------------------
-- 3.4.5 supplier_permission 供应商权限
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_permission`;
CREATE TABLE `supplier_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `parent_code` VARCHAR(50) DEFAULT NULL COMMENT '父权限编码（NULL=顶级）',
  `permission_type` TINYINT(2) NOT NULL COMMENT '权限类型（1=菜单, 2=按钮, 3=接口, 4=数据）',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由/接口路径',
  `method` VARCHAR(20) DEFAULT NULL COMMENT '请求方法',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_code` (`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商权限';

-- ---------------------------------------------------------------------
-- 3.4.6 supplier_role_permission_ship 供应商角色权限对照
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_role_permission_ship`;
CREATE TABLE `supplier_role_permission_ship` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `permission_code` VARCHAR(64) NOT NULL COMMENT '权限编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_code`, `permission_code`),
  KEY `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商角色权限对照';

-- ---------------------------------------------------------------------
-- 3.4.7 supplier_contract 供应商合同
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_contract`;
CREATE TABLE `supplier_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contract_code` VARCHAR(50) NOT NULL COMMENT '合同编号',
  `contract_name` VARCHAR(200) NOT NULL COMMENT '合同名称',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '签约组织编码',
  `contract_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '合同类型（1=合作框架协议, 2=年度合同, 3=单次合同, 4=补充协议）',
  `sign_date` DATE DEFAULT NULL COMMENT '签约日期',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  `expire_date` DATE NOT NULL COMMENT '到期日期',
  `contract_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '合同金额',
  `commission_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '佣金比例',
  `settlement_cycle` TINYINT(2) NOT NULL DEFAULT 2 COMMENT '结算周期（1=月结, 2=季结, 3=半年结, 4=年结）',
  `terms` TEXT DEFAULT NULL COMMENT '合同条款',
  `attachment_urls` TEXT DEFAULT NULL COMMENT '合同附件URL（JSON数组）',
  `sign_person` VARCHAR(50) DEFAULT NULL COMMENT '签约人',
  `sign_seal_image` VARCHAR(500) DEFAULT NULL COMMENT '签约盖章图片URL',
  `is_auto_renew` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动续约（0=否, 1=是）',
  `renew_count` INT(11) NOT NULL DEFAULT 0 COMMENT '续约次数',
  `parent_contract_code` VARCHAR(64) DEFAULT NULL COMMENT '原合同编码（续约时关联）',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=草稿, 1=待审核, 2=已生效, 3=已到期, 4=已终止, 5=已作废）',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_code` (`contract_code`),
  KEY `idx_supplier_code` (`supplier_code`),
  KEY `idx_organ_code` (`organ_code`),
  KEY `idx_effective_date` (`effective_date`),
  KEY `idx_expire_date` (`expire_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商合同';

-- ---------------------------------------------------------------------
-- 3.4.8 supplier_evaluation 供应商评价
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_evaluation`;
CREATE TABLE `supplier_evaluation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码',
  `eval_period` VARCHAR(20) NOT NULL COMMENT '评价周期（如 2025-Q1）',
  `eval_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '评价类型（1=定期评估, 2=临时评估, 3=客户投诉触发）',
  `service_quality_score` DECIMAL(5,2) DEFAULT NULL COMMENT '服务质量评分（0-100）',
  `facility_quality_score` DECIMAL(5,2) DEFAULT NULL COMMENT '设施质量评分（0-100）',
  `cooperation_score` DECIMAL(5,2) DEFAULT NULL COMMENT '配合度评分（0-100）',
  `complaint_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '投诉率',
  `total_order_count` INT(11) DEFAULT 0 COMMENT '期间订单总量',
  `complaint_count` INT(11) DEFAULT 0 COMMENT '期间投诉量',
  `total_score` DECIMAL(5,2) DEFAULT NULL COMMENT '综合评分（0-100）',
  `score_level` TINYINT(2) DEFAULT NULL COMMENT '评分等级（1=A级, 2=B级, 3=C级, 4=D级）',
  `eval_content` TEXT DEFAULT NULL COMMENT '评价内容',
  `improvement_suggestions` TEXT DEFAULT NULL COMMENT '改进建议',
  `evaluator_code` VARCHAR(64) DEFAULT NULL COMMENT '评价人编码',
  `evaluator_name` VARCHAR(50) DEFAULT NULL COMMENT '评价人姓名',
  `eval_date` DATE NOT NULL COMMENT '评价日期',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=草稿, 1=已提交）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_code` (`supplier_code`),
  KEY `idx_eval_period` (`eval_period`),
  KEY `idx_eval_date` (`eval_date`),
  KEY `idx_score_level` (`score_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商评价';

-- ---------------------------------------------------------------------
-- 3.4.9 supplier_account_role_rel 供应商账号角色关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_account_role_rel`;
CREATE TABLE `supplier_account_role_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account_code` VARCHAR(50) NOT NULL COMMENT '账号编码',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_role` (`account_code`, `role_code`),
  KEY `idx_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商账号角色关联';

-- ---------------------------------------------------------------------
-- 3.4.10 supplier_contact 供应商联系人
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `supplier_contact`;
CREATE TABLE `supplier_contact` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码',
  `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  `contact_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '联系人类型（1=商务, 2=财务, 3=技术, 4=运营, 5=其他）',
  `position` VARCHAR(100) DEFAULT NULL COMMENT '职位',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `wechat` VARCHAR(50) DEFAULT NULL COMMENT '微信号',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主联系人（0=否, 1=是）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_code` (`supplier_code`),
  KEY `idx_contact_type` (`contact_type`),
  KEY `idx_is_primary` (`is_primary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商联系人';
