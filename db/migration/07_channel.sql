-- =====================================================================
-- 07_channel.sql  渠道域（channel_）
-- 域说明：保险公司等合作方信息、开放平台对接、内容/场景/商品配置、RBAC五表、数据同步日志
-- 表数：11
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.7
-- 主键策略：全部为平台共享表（AUTO_INCREMENT，渠道域属广播表，不参与分片）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.7.1 channel_info 渠道信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_info`;
CREATE TABLE `channel_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码（CH+5位数字）',
  `full_name` VARCHAR(200) NOT NULL COMMENT '渠道名称',
  `short_name` VARCHAR(50) DEFAULT NULL COMMENT '简称',
  `channel_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '渠道类型（1=保险公司总部, 2=保险公司分公司, 3=保险支公司, 4=银行, 5=其他金融机构）',
  `parent_code` VARCHAR(50) DEFAULT NULL COMMENT '上级渠道编码（用于上下级关系）',
  `ancestors` VARCHAR(500) DEFAULT NULL COMMENT '祖级列表',
  `level` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '层级（1=一级, 2=二级, 3=三级）',
  `unified_credit_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
  `legal_person` VARCHAR(50) DEFAULT NULL COMMENT '法定代表人',
  `province_code` VARCHAR(20) DEFAULT NULL COMMENT '省份编码',
  `city_code` VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
  `district_code` VARCHAR(20) DEFAULT NULL COMMENT '区划编码',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `contact_email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  `logo_url` VARCHAR(500) DEFAULT NULL COMMENT 'Logo URL',
  `description` TEXT DEFAULT NULL COMMENT '渠道介绍',
  `agent_count` INT(11) NOT NULL DEFAULT 0 COMMENT '旗下代理人数量',
  `total_order_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '累计订单金额',
  `cooperation_start_date` DATE DEFAULT NULL COMMENT '合作开始日期',
  `distributor_code` VARCHAR(50) DEFAULT NULL COMMENT '分销商编码（关联 distributor_info，无分销商时为空）',
  `settlement_cycle` TINYINT(2) NOT NULL DEFAULT 2 COMMENT '结算周期（1=月结, 2=季结）',
  `feature_config` TEXT DEFAULT NULL COMMENT '渠道功能开关配置（JSON格式）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待审核, 1=合作中, 2=已暂停, 3=已终止）',
  `audit_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '审核状态（0=待审核, 1=审核通过, 2=审核驳回）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`),
  KEY `idx_full_name` (`full_name`(50)),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_channel_type` (`channel_type`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_status` (`status`),
  KEY `idx_distributor_code` (`distributor_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道信息';

-- ---------------------------------------------------------------------
-- 3.7.2 channel_open_platform 渠道开放平台
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_open_platform`;
CREATE TABLE `channel_open_platform` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `platform_name` VARCHAR(100) NOT NULL COMMENT '平台名称',
  `dock_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '对接类型（1=H5嵌入, 2=API对接, 3=SDK集成）',
  `api_base_url` VARCHAR(500) DEFAULT NULL COMMENT 'API基础地址',
  `app_key` VARCHAR(100) DEFAULT NULL COMMENT '应用Key',
  `app_secret` VARCHAR(200) DEFAULT NULL COMMENT '应用密钥（加密存储）',
  `callback_url` VARCHAR(500) DEFAULT NULL COMMENT '回调地址',
  `h5_domain` VARCHAR(200) DEFAULT NULL COMMENT 'H5域名配置',
  `h5_theme` VARCHAR(50) DEFAULT NULL COMMENT 'H5主题配置',
  `auth_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '认证方式（1=Token, 2=签名）',
  `ip_whitelist` TEXT DEFAULT NULL COMMENT 'IP白名单（JSON数组）',
  `rate_limit` INT(11) DEFAULT 1000 COMMENT '调用频率限制（次/分钟）',
  `timeout` INT(11) DEFAULT 30 COMMENT '超时时间（秒）',
  `extra_config` TEXT DEFAULT NULL COMMENT '扩展配置（JSON格式）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_channel_code` (`channel_code`),
  UNIQUE KEY `uk_app_key` (`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道开放平台';

-- ---------------------------------------------------------------------
-- 3.7.3 channel_account 渠道账号
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_account`;
CREATE TABLE `channel_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
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
  `account_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '账号状态（0=锁定, 1=正常, 2=禁用）',
  `is_admin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否管理员（0=否, 1=是）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_code` (`account_code`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道账号';

-- ---------------------------------------------------------------------
-- 3.7.4 channel_role 渠道角色
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_role`;
CREATE TABLE `channel_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '所属渠道编码',
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
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道角色';

-- ---------------------------------------------------------------------
-- 3.7.5 channel_permission 渠道权限
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_permission`;
CREATE TABLE `channel_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `parent_code` VARCHAR(50) DEFAULT NULL COMMENT '父权限编码（NULL=顶级）',
  `permission_type` TINYINT(2) NOT NULL COMMENT '权限类型（1=菜单, 2=按钮, 3=接口, 4=数据）',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由/接口路径',
  `method` VARCHAR(20) DEFAULT NULL COMMENT '请求方法',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_code` (`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道权限';

-- ---------------------------------------------------------------------
-- 3.7.6 channel_role_permission_ship 渠道角色权限关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_role_permission_ship`;
CREATE TABLE `channel_role_permission_ship` (
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
  UNIQUE KEY `uk_role_permission` (`role_code`, `permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道角色权限关联';

-- ---------------------------------------------------------------------
-- 3.7.7 channel_config_content 渠道内容配置
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_config_content`;
CREATE TABLE `channel_config_content` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `content_code` VARCHAR(64) NOT NULL COMMENT '内容编码',
  `content_type` TINYINT(2) NOT NULL COMMENT '内容类型（1=文章, 2=视频, 3=图片, 4=专题）',
  `app_type` VARCHAR(10) NOT NULL DEFAULT 'agent' COMMENT '展示端类型（agent=代理人端, client=客户端）',
  `position` VARCHAR(50) NOT NULL COMMENT '展示位置（banner/recommend/hot/new等）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶（0=否, 1=是）',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '失效时间',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_content_code` (`content_code`),
  KEY `idx_position` (`position`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道内容配置';

-- ---------------------------------------------------------------------
-- 3.7.8 channel_config_scene 渠道场景配置
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_config_scene`;
CREATE TABLE `channel_config_scene` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `scene_code` VARCHAR(64) NOT NULL COMMENT '场景编码',
  `is_exclusive` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否渠道专属（0=否, 1=是）',
  `custom_name` VARCHAR(200) DEFAULT NULL COMMENT '自定义场景名称（渠道定制）',
  `custom_price` DECIMAL(12,2) DEFAULT NULL COMMENT '自定义价格',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '失效时间',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_scene_code` (`scene_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道场景配置';

-- ---------------------------------------------------------------------
-- 3.7.9 channel_config_goods 渠道商品配置
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_config_goods`;
CREATE TABLE `channel_config_goods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `goods_code` VARCHAR(64) NOT NULL COMMENT '商品编码',
  `goods_type` TINYINT(2) NOT NULL COMMENT '商品类型（1=权益商品, 2=场景商品, 3=课程商品, 4=旅居商品）',
  `custom_name` VARCHAR(200) DEFAULT NULL COMMENT '自定义商品名称',
  `custom_price` DECIMAL(12,2) DEFAULT NULL COMMENT '自定义价格',
  `custom_description` TEXT DEFAULT NULL COMMENT '自定义描述',
  `is_exclusive` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否渠道专属（0=否, 1=是）',
  `purchase_limit` INT(11) DEFAULT NULL COMMENT '采购限制数量（NULL=不限）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '失效时间',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_goods_code` (`goods_code`),
  KEY `idx_goods_type` (`goods_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道商品配置';

-- ---------------------------------------------------------------------
-- 3.7.10 channel_account_role_rel 渠道账号角色关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_account_role_rel`;
CREATE TABLE `channel_account_role_rel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account_code` VARCHAR(50) NOT NULL COMMENT '账号编码',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_role` (`account_code`, `role_code`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道账号角色关联';

-- ---------------------------------------------------------------------
-- 3.7.11 channel_data_sync_log 渠道数据同步日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `channel_data_sync_log`;
CREATE TABLE `channel_data_sync_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sync_code` VARCHAR(64) NOT NULL COMMENT '同步记录编码',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `sync_type` TINYINT(2) NOT NULL COMMENT '同步类型（1=内容推送, 2=场景推送, 3=权益同步, 4=订单回传, 5=客户回传）',
  `biz_code` VARCHAR(64) DEFAULT NULL COMMENT '业务编码（对应同步对象）',
  `direction` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '方向（1=推送至渠道, 2=渠道回调）',
  `request_data` TEXT DEFAULT NULL COMMENT '请求报文（JSON）',
  `response_data` TEXT DEFAULT NULL COMMENT '响应报文（JSON）',
  `http_status` INT(11) DEFAULT NULL COMMENT 'HTTP状态码',
  `result` TINYINT(2) DEFAULT NULL COMMENT '结果（0=失败, 1=成功）',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
  `retry_count` INT(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `sync_time` DATETIME NOT NULL COMMENT '同步时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_code` (`sync_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_result` (`result`),
  KEY `idx_sync_time` (`sync_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道数据同步日志';
