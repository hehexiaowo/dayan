-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 02_organ.sql  核心域（organ_）
-- 域说明：平台运营方（大雁养老）的组织信息、账号体系和权限控制（RBAC五表）
-- 表数：9
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.2
-- 主键策略：全部为平台共享表（AUTO_INCREMENT）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.2.1 organ_info 公司信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_info`;
CREATE TABLE `organ_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '组织编码（OG+5位数字）',
  `full_name` VARCHAR(200) NOT NULL COMMENT '组织全称',
  `short_name` VARCHAR(50) DEFAULT NULL COMMENT '简称',
  `organ_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '组织类型（1=运营方, 2=子公司, 3=分公司）',
  `unified_credit_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
  `legal_person` VARCHAR(50) DEFAULT NULL COMMENT '法定代表人',
  `registered_capital` DECIMAL(12,2) DEFAULT NULL COMMENT '注册资本（万元）',
  `establish_date` DATE DEFAULT NULL COMMENT '成立日期',
  `business_scope` TEXT DEFAULT NULL COMMENT '经营范围',
  `province_code` VARCHAR(20) DEFAULT NULL COMMENT '省份编码',
  `city_code` VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
  `district_code` VARCHAR(20) DEFAULT NULL COMMENT '区划编码',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `contact_email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  `logo_url` VARCHAR(500) DEFAULT NULL COMMENT 'Logo图片URL',
  `website` VARCHAR(200) DEFAULT NULL COMMENT '官网地址',
  `description` TEXT DEFAULT NULL COMMENT '组织介绍',
  `license_image` VARCHAR(500) DEFAULT NULL COMMENT '营业执照图片URL',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_organ_code` (`organ_code`),
  KEY `idx_full_name` (`full_name`(50)),
  KEY `idx_unified_credit_code` (`unified_credit_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司信息';

-- ---------------------------------------------------------------------
-- 3.2.2 organ_account 核心账号
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_account`;
CREATE TABLE `organ_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '所属组织编码',
  `account_code` VARCHAR(50) NOT NULL COMMENT '账号编码',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
  `password` VARCHAR(200) NOT NULL COMMENT '密码（加密存储）',
  `salt` VARCHAR(50) NOT NULL COMMENT '密码盐值',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `gender` TINYINT(1) DEFAULT 0 COMMENT '性别（0=未知, 1=男, 2=女）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `open_id` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID（支持微信登录）',
  `union_id` VARCHAR(100) DEFAULT NULL COMMENT '微信UnionID（支持微信登录）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号（加密存储）',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_count` INT(11) NOT NULL DEFAULT 0 COMMENT '累计登录次数',
  `pwd_update_time` DATETIME DEFAULT NULL COMMENT '密码修改时间',
  `account_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '账号状态（0=锁定, 1=正常, 2=禁用）',
  `is_admin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否超级管理员（0=否, 1=是）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_code` (`account_code`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_organ_code` (`organ_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`),
  KEY `idx_account_status` (`account_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心账号';

-- ---------------------------------------------------------------------
-- 3.2.3 organ_role 核心角色
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_role`;
CREATE TABLE `organ_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '所属组织编码',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `role_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '角色类型（1=系统预置, 2=自定义）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
  `data_scope` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '数据范围（1=全部, 2=本部门及下级, 3=本部门, 4=仅本人）',
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
  KEY `idx_organ_code` (`organ_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心角色';

-- ---------------------------------------------------------------------
-- 3.2.4 organ_permission 核心权限
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_permission`;
CREATE TABLE `organ_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码（如 organ:park:list）',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `parent_code` VARCHAR(50) DEFAULT NULL COMMENT '父权限编码（NULL=顶级）',
  `permission_type` TINYINT(2) NOT NULL COMMENT '权限类型（1=菜单, 2=按钮, 3=接口, 4=数据）',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由/接口路径',
  `method` VARCHAR(20) DEFAULT NULL COMMENT '请求方法（GET/POST/PUT/DELETE）',
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
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_permission_type` (`permission_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心权限';

-- ---------------------------------------------------------------------
-- 3.2.5 organ_role_permission_ship 核心角色权限对照
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_role_permission_ship`;
CREATE TABLE `organ_role_permission_ship` (
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
  KEY `idx_role_code` (`role_code`),
  KEY `idx_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心角色权限对照';

-- ---------------------------------------------------------------------
-- 3.2.6 organ_department 组织架构/部门
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_department`;
CREATE TABLE `organ_department` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '所属组织编码',
  `dept_code` VARCHAR(50) NOT NULL COMMENT '部门编码',
  `dept_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
  `parent_code` VARCHAR(50) DEFAULT NULL COMMENT '父部门编码（NULL=顶级）',
  `ancestors` VARCHAR(500) DEFAULT NULL COMMENT '祖级列表（如 0,1,5）',
  `dept_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '部门类型（1=公司, 2=部门, 3=小组）',
  `leader_name` VARCHAR(50) DEFAULT NULL COMMENT '负责人姓名',
  `leader_phone` VARCHAR(20) DEFAULT NULL COMMENT '负责人电话',
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
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_organ_code` (`organ_code`),
  KEY `idx_parent_code` (`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织架构/部门';

-- ---------------------------------------------------------------------
-- 3.2.7 organ_employee 员工信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_employee`;
CREATE TABLE `organ_employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '所属组织编码',
  `employee_code` VARCHAR(50) NOT NULL COMMENT '员工编码（工号）',
  `account_code` VARCHAR(64) DEFAULT NULL COMMENT '关联账号编码',
  `dept_code` VARCHAR(64) DEFAULT NULL COMMENT '所属部门编码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `gender` TINYINT(1) DEFAULT 0 COMMENT '性别（0=未知, 1=男, 2=女）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号（加密存储）',
  `position` VARCHAR(100) DEFAULT NULL COMMENT '职位',
  `entry_date` DATE DEFAULT NULL COMMENT '入职日期',
  `leave_date` DATE DEFAULT NULL COMMENT '离职日期',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `employee_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=离职, 1=在职, 2=试用期）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_code` (`employee_code`),
  KEY `idx_organ_code` (`organ_code`),
  KEY `idx_dept_code` (`dept_code`),
  KEY `idx_account_code` (`account_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工信息';

-- ---------------------------------------------------------------------
-- 3.2.8 organ_account_role_rel 核心账号角色关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_account_role_rel`;
CREATE TABLE `organ_account_role_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account_code` VARCHAR(50) NOT NULL COMMENT '账号编码',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '所属组织编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_role` (`account_code`, `role_code`),
  KEY `idx_account_code` (`account_code`),
  KEY `idx_role_code` (`role_code`),
  KEY `idx_organ_code` (`organ_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心账号角色关联';

-- ---------------------------------------------------------------------
-- 3.2.9 organ_role_menu_rel 角色菜单关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `organ_role_menu_rel`;
CREATE TABLE `organ_role_menu_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `menu_code` VARCHAR(50) NOT NULL COMMENT '菜单编码（关联 system_menu.menu_code）',
  `organ_code` VARCHAR(50) DEFAULT NULL COMMENT '所属组织编码（NULL=全平台角色）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_code`, `menu_code`),
  KEY `idx_role_code` (`role_code`),
  KEY `idx_menu_code` (`menu_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联';
