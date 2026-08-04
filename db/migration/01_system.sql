-- =====================================================================
-- 01_system.sql  系统域（system_）
-- 域说明：平台基础支撑能力（数据字典、状态机、日志、消息、配置、菜单等）
-- 表数：18
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.1
-- 主键策略：system_message_read 为分片表（雪花ID），其余 17 表为平台共享表（AUTO_INCREMENT）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.1.1 system_dict_common 系统基本字典
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_dict_common`;
CREATE TABLE `system_dict_common` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type` VARCHAR(50) NOT NULL COMMENT '字典类型（如 gender、status、education）',
  `dict_code` VARCHAR(50) NOT NULL COMMENT '字典编码（类型内唯一）',
  `dict_name` VARCHAR(100) NOT NULL COMMENT '字典显示名称',
  `dict_value` VARCHAR(100) NOT NULL COMMENT '字典存储值',
  `parent_code` VARCHAR(50) DEFAULT NULL COMMENT '父级编码（树形结构）',
  `level` TINYINT(2) DEFAULT 1 COMMENT '层级（1-一级, 2-二级, 3-三级）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标（前端展示用）',
  `css_class` VARCHAR(100) DEFAULT NULL COMMENT '样式类名',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认项（0=否, 1=是）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`dict_type`, `dict_code`),
  KEY `idx_dict_type` (`dict_type`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统基本字典';

-- ---------------------------------------------------------------------
-- 3.1.2 system_dict_region 系统地域字典
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_dict_region`;
CREATE TABLE `system_dict_region` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `region_code` VARCHAR(20) NOT NULL COMMENT '行政区划代码（国标）',
  `region_name` VARCHAR(100) NOT NULL COMMENT '区划名称',
  `parent_code` VARCHAR(20) DEFAULT NULL COMMENT '父级区划代码',
  `level` TINYINT(2) NOT NULL COMMENT '层级（1=省, 2=市, 3=区/县）',
  `pinyin` VARCHAR(200) DEFAULT NULL COMMENT '拼音（用于搜索排序）',
  `first_letter` CHAR(1) DEFAULT NULL COMMENT '首字母',
  `lng` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_region_code` (`region_code`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_level` (`level`),
  KEY `idx_first_letter` (`first_letter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统地域字典';

-- ---------------------------------------------------------------------
-- 3.1.3 system_dict_iplocation 系统IP地址地域字典
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_dict_iplocation`;
CREATE TABLE `system_dict_iplocation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ip_start` VARCHAR(50) NOT NULL COMMENT 'IP起始地址',
  `ip_end` VARCHAR(50) NOT NULL COMMENT 'IP结束地址',
  `ip_start_num` BIGINT(20) NOT NULL COMMENT 'IP起始数值（用于快速查询）',
  `ip_end_num` BIGINT(20) NOT NULL COMMENT 'IP结束数值',
  `country` VARCHAR(50) DEFAULT NULL COMMENT '国家',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `district` VARCHAR(50) DEFAULT NULL COMMENT '区/县',
  `isp` VARCHAR(100) DEFAULT NULL COMMENT '运营商',
  `region_code` VARCHAR(20) DEFAULT NULL COMMENT '关联区划代码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_ip_start_num` (`ip_start_num`),
  KEY `idx_ip_end_num` (`ip_end_num`),
  KEY `idx_region_code` (`region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统IP地址地域字典';

-- ---------------------------------------------------------------------
-- 3.1.4 system_state_machine 状态机
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_state_machine`;
CREATE TABLE `system_state_machine` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `machine_code` VARCHAR(50) NOT NULL COMMENT '状态机编码（如 ORDER_SM、EQUITY_SM、SERVICE_SESSION_SM）',
  `machine_name` VARCHAR(100) NOT NULL COMMENT '状态机名称',
  `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型（order、equity、service等）',
  `from_state` TINYINT(4) NOT NULL COMMENT '源主状态值',
  `from_state_name` VARCHAR(50) NOT NULL COMMENT '源主状态名称',
  `from_sub_state` VARCHAR(20) DEFAULT NULL COMMENT '源子状态值（单维状态机留空）',
  `to_state` TINYINT(4) NOT NULL COMMENT '目标主状态值',
  `to_state_name` VARCHAR(50) NOT NULL COMMENT '目标主状态名称',
  `to_sub_state` VARCHAR(20) DEFAULT NULL COMMENT '目标子状态值（单维状态机留空）',
  `event_code` VARCHAR(50) NOT NULL COMMENT '触发事件编码',
  `event_name` VARCHAR(100) NOT NULL COMMENT '触发事件名称',
  `condition_expr` VARCHAR(500) DEFAULT NULL COMMENT '流转条件表达式',
  `action_bean` VARCHAR(200) DEFAULT NULL COMMENT '流转执行器（后端类名）',
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
  UNIQUE KEY `uk_machine_from_event` (`machine_code`, `from_state`, `from_sub_state`, `event_code`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='状态机';

-- ---------------------------------------------------------------------
-- 3.1.5 system_log_organ 核心日志-组织操作
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_log_organ`;
CREATE TABLE `system_log_organ` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `organ_code` VARCHAR(50) NOT NULL COMMENT '组织编码',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作（create/update/delete/audit等）',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '操作对象编码',
  `content` TEXT DEFAULT NULL COMMENT '操作内容描述',
  `before_data` TEXT DEFAULT NULL COMMENT '变更前数据（JSON格式）',
  `after_data` TEXT DEFAULT NULL COMMENT '变更后数据（JSON格式）',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法（GET/POST等）',
  `result_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '结果状态（0=失败, 1=成功）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `duration` INT(11) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_organ_code` (`organ_code`),
  KEY `idx_account_code` (`account_code`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_target` (`target_type`, `target_code`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心日志-组织操作';

-- ---------------------------------------------------------------------
-- 3.1.6 system_log_supplier 供应商日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_log_supplier`;
CREATE TABLE `system_log_supplier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '操作对象编码',
  `content` TEXT DEFAULT NULL COMMENT '操作内容描述',
  `before_data` TEXT DEFAULT NULL COMMENT '变更前数据（JSON格式）',
  `after_data` TEXT DEFAULT NULL COMMENT '变更后数据（JSON格式）',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `result_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '结果状态（0=失败, 1=成功）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_code` (`supplier_code`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商日志';

-- ---------------------------------------------------------------------
-- 3.1.7 system_log_channel 渠道日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_log_channel`;
CREATE TABLE `system_log_channel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '操作对象编码',
  `content` TEXT DEFAULT NULL COMMENT '操作内容描述',
  `before_data` TEXT DEFAULT NULL COMMENT '变更前数据（JSON格式）',
  `after_data` TEXT DEFAULT NULL COMMENT '变更后数据（JSON格式）',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `result_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '结果状态（0=失败, 1=成功）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道日志';

-- ---------------------------------------------------------------------
-- 3.1.8 system_message_template 消息模板
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_message_template`;
CREATE TABLE `system_message_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码（全局唯一）',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型（register/login/activate/notify/remind/refund等）',
  `channel_type` TINYINT(2) NOT NULL COMMENT '渠道类型（1=短信, 2=站内信, 3=APP推送, 4=企业微信, 5=微信模板消息, 6=邮件）',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '消息标题（站内信/推送/邮件必填）',
  `content` TEXT NOT NULL COMMENT '模板正文（含变量占位符 ${var}）',
  `variables` TEXT DEFAULT NULL COMMENT '变量定义（JSON数组）',
  `channel_config` TEXT DEFAULT NULL COMMENT '渠道差异配置（JSON）',
  `fallback_channel_type` TINYINT(2) DEFAULT NULL COMMENT '降级渠道（本渠道发送失败时备选渠道）',
  `channel_code` VARCHAR(50) DEFAULT NULL COMMENT '渠道编码（NULL=平台通用模板）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_biz_channel` (`biz_type`, `channel_type`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息模板';

-- ---------------------------------------------------------------------
-- 3.1.9 system_config 系统配置
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_group` VARCHAR(50) NOT NULL COMMENT '配置分组（system、sms、oss、payment 等）',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT DEFAULT NULL COMMENT '配置值（敏感配置加密存储）',
  `value_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型（string/number/boolean/json）',
  `env` VARCHAR(20) NOT NULL DEFAULT 'prod' COMMENT '环境（dev/sit/uat/prod）',
  `scope` VARCHAR(20) NOT NULL DEFAULT 'global' COMMENT '配置作用域（global/organ/user）',
  `organ_code` VARCHAR(50) DEFAULT NULL COMMENT '组织编码（scope=organ 时必填）',
  `user_code` VARCHAR(50) DEFAULT NULL COMMENT '用户/账号编码（scope=user 时必填）',
  `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称（显示用）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '配置说明',
  `is_secret` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否敏感配置（0=否, 1=是）',
  `is_runtime` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否运行时热更新（0=需重启, 1=运行时生效）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_key_env_scope` (`config_group`, `config_key`, `env`, `scope`, `organ_code`, `user_code`),
  KEY `idx_organ_code` (`organ_code`),
  KEY `idx_env_scope` (`env`, `scope`),
  KEY `idx_is_secret` (`is_secret`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- ---------------------------------------------------------------------
-- 3.1.10 system_config_log 系统配置变更历史
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_config_log`;
CREATE TABLE `system_config_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` BIGINT(20) NOT NULL COMMENT '关联 system_config.id',
  `config_group` VARCHAR(50) NOT NULL COMMENT '配置分组（冗余，便于查询）',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键（冗余）',
  `env` VARCHAR(20) NOT NULL COMMENT '环境',
  `old_value` TEXT DEFAULT NULL COMMENT '变更前值（敏感配置脱敏）',
  `new_value` TEXT DEFAULT NULL COMMENT '变更后值（敏感配置脱敏）',
  `action` VARCHAR(20) NOT NULL COMMENT '操作类型（create/update/delete/refresh）',
  `account_type` VARCHAR(30) NOT NULL COMMENT '操作账号类型',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_config_key_env` (`config_key`, `env`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置变更历史';

-- ---------------------------------------------------------------------
-- 3.1.11 system_menu 菜单管理
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_menu`;
CREATE TABLE `system_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `menu_code` VARCHAR(50) NOT NULL COMMENT '菜单编码',
  `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `parent_code` VARCHAR(50) DEFAULT NULL COMMENT '父菜单编码（NULL=顶级菜单）',
  `menu_type` TINYINT(2) NOT NULL COMMENT '菜单类型（1=目录, 2=菜单, 3=按钮, 4=接口）',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
  `permission_code` VARCHAR(100) DEFAULT NULL COMMENT '权限标识（如 organ:park:list）',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可见（0=隐藏, 1=显示）',
  `is_external` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否外链（0=否, 1=是）',
  `is_cache` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否缓存（0=否, 1=是）',
  `domain_type` VARCHAR(30) NOT NULL COMMENT '所属域（organ/butler/supplier/channel）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_domain_type` (`domain_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单管理';

-- ---------------------------------------------------------------------
-- 3.1.12 system_operation_log 操作审计日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_operation_log`;
CREATE TABLE `system_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `account_type` VARCHAR(30) NOT NULL COMMENT '账号类型（organ/butler/supplier/channel/agent/client）',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作',
  `action_description` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '操作对象编码',
  `target_description` VARCHAR(200) DEFAULT NULL COMMENT '操作对象描述',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数（脱敏后）',
  `response_code` INT(11) DEFAULT NULL COMMENT '响应状态码',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `ip_location` VARCHAR(200) DEFAULT NULL COMMENT 'IP归属地',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型（pc/mobile/tablet）',
  `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
  `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器',
  `result_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '结果（0=失败, 1=成功）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `duration` INT(11) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_account` (`account_type`, `account_code`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_target` (`target_type`, `target_code`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志';

-- ---------------------------------------------------------------------
-- 3.1.13 system_login_log 客户登录日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_login_log`;
CREATE TABLE `system_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `client_code` VARCHAR(50) DEFAULT NULL COMMENT '客户编码（登录成功后填充）',
  `login_type` TINYINT(2) NOT NULL COMMENT '登录方式（1=手机号, 2=微信, 3=支付宝, 4=账号密码）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '登录手机号（脱敏）',
  `open_id` VARCHAR(100) DEFAULT NULL COMMENT '第三方OpenID',
  `login_ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
  `login_location` VARCHAR(100) DEFAULT NULL COMMENT '登录地域（IP解析）',
  `device_type` TINYINT(2) DEFAULT NULL COMMENT '设备类型（1=Android, 2=iOS, 3=H5, 4=小程序, 5=PC）',
  `device_info` VARCHAR(500) DEFAULT NULL COMMENT '设备信息（UA等）',
  `result` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '结果（0=失败, 1=成功）',
  `fail_reason` VARCHAR(200) DEFAULT NULL COMMENT '失败原因',
  `login_time` DATETIME NOT NULL COMMENT '登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_ip` (`login_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户登录日志';

-- ---------------------------------------------------------------------
-- 3.1.14 system_service_change_log 服务变更日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_service_change_log`;
CREATE TABLE `system_service_change_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_code` VARCHAR(64) NOT NULL COMMENT '服务会话编码',
  `change_type` TINYINT(2) NOT NULL COMMENT '变更类型（1=状态变更, 2=管家变更, 3=方案变更, 4=安排变更, 5=时间变更）',
  `from_value` VARCHAR(500) DEFAULT NULL COMMENT '变更前值',
  `to_value` VARCHAR(500) DEFAULT NULL COMMENT '变更后值',
  `change_reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  `operator_code` VARCHAR(64) NOT NULL COMMENT '操作人编码',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `operator_type` VARCHAR(30) NOT NULL COMMENT '操作人类型（butler/client/system/admin）',
  `operate_time` DATETIME NOT NULL COMMENT '操作时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_code` (`session_code`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务变更日志';

-- ---------------------------------------------------------------------
-- 3.1.15 system_order_status_log 订单状态变更日志
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_order_status_log`;
CREATE TABLE `system_order_status_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_type` TINYINT(2) NOT NULL COMMENT '订单类型',
  `order_code` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `from_status` TINYINT(4) NOT NULL COMMENT '原状态',
  `to_status` TINYINT(4) NOT NULL COMMENT '新状态',
  `change_reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  `operator_code` VARCHAR(64) DEFAULT NULL COMMENT '操作人编码',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `operator_type` VARCHAR(30) NOT NULL COMMENT '操作人类型（system/channel/agent/client/admin）',
  `operate_time` DATETIME NOT NULL COMMENT '操作时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_type`, `order_code`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态变更日志';

-- ---------------------------------------------------------------------
-- 3.1.16 system_message 消息发送记录
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_message`;
CREATE TABLE `system_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_code` VARCHAR(50) NOT NULL COMMENT '消息实例编码（全局唯一）',
  `batch_code` VARCHAR(50) DEFAULT NULL COMMENT '发送批次编码',
  `template_code` VARCHAR(50) DEFAULT NULL COMMENT '关联 system_message_template.template_code',
  `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型（与模板 biz_type 对应）',
  `channel_type` TINYINT(2) NOT NULL COMMENT '实际发送渠道（1=短信, 2=站内信, 3=APP推送, 4=企微, 5=微信模板, 6=邮件）',
  `message_type` TINYINT(2) NOT NULL COMMENT '消息类型（1=系统通知, 2=业务提醒, 3=活动通知, 4=权益通知, 5=服务通知）',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '消息标题（渲染后）',
  `content` TEXT NOT NULL COMMENT '消息正文（模板渲染 + 变量替换后的最终内容）',
  `target_type` VARCHAR(30) NOT NULL COMMENT '接收者类型（organ/butler/supplier/channel/agent/client）',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '接收者编码',
  `target_name` VARCHAR(100) DEFAULT NULL COMMENT '接收者名称（冗余）',
  `target_contact` VARCHAR(100) DEFAULT NULL COMMENT '接收者联系方式（手机号/openid/邮箱）',
  `sender_type` VARCHAR(30) NOT NULL DEFAULT 'system' COMMENT '发送者类型（system/organ/butler/channel）',
  `sender_code` VARCHAR(50) DEFAULT NULL COMMENT '发送者编码',
  `link_url` VARCHAR(500) DEFAULT NULL COMMENT '跳转链接（渲染后）',
  `link_type` TINYINT(2) DEFAULT NULL COMMENT '链接类型（1=内部页面, 2=外部链接, 3=APP路由）',
  `send_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '发送状态（0=待发送, 1=发送中, 2=发送成功, 3=发送失败, 4=已送达, 5=已读, 6=已撤回）',
  `provider_msg_id` VARCHAR(100) DEFAULT NULL COMMENT '第三方服务商消息ID',
  `send_time` DATETIME DEFAULT NULL COMMENT '实际发送时间',
  `deliver_time` DATETIME DEFAULT NULL COMMENT '送达时间（第三方回执确认）',
  `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（超时未送达则标记失败）',
  `retry_count` INT(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `error_code` VARCHAR(50) DEFAULT NULL COMMENT '失败错误码',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `priority` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '优先级（0=普通, 1=重要, 2=紧急）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_code` (`message_code`),
  KEY `idx_batch` (`batch_code`),
  KEY `idx_template` (`template_code`),
  KEY `idx_biz_channel` (`biz_type`, `channel_type`),
  KEY `idx_target` (`target_type`, `target_code`),
  KEY `idx_send_status` (`send_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息发送记录';

-- ---------------------------------------------------------------------
-- 3.1.17 system_message_read 消息已读记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_message_read`;
CREATE TABLE `system_message_read` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `message_code` VARCHAR(50) NOT NULL COMMENT '消息编码',
  `account_type` VARCHAR(30) NOT NULL COMMENT '账号类型',
  `account_code` VARCHAR(50) NOT NULL COMMENT '接收者编码',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读（0=未读, 1=已读）',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_account` (`message_code`, `account_type`, `account_code`),
  KEY `idx_account` (`account_type`, `account_code`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读记录';

-- ---------------------------------------------------------------------
-- 3.1.18 system_dict_business 业务字典
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_dict_business`;
CREATE TABLE `system_dict_business` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type` VARCHAR(64) NOT NULL COMMENT '字典类型（如 equity_type、service_type、order_type、flow_type）',
  `dict_code` VARCHAR(64) NOT NULL COMMENT '字典编码（同类型下唯一）',
  `dict_name` VARCHAR(128) NOT NULL COMMENT '字典显示名称',
  `dict_value` VARCHAR(128) DEFAULT NULL COMMENT '字典存储值',
  `parent_code` VARCHAR(64) DEFAULT NULL COMMENT '父级编码（支持树形）',
  `domain` VARCHAR(32) DEFAULT NULL COMMENT '所属业务域',
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
  UNIQUE KEY `uk_type_code` (`dict_type`, `dict_code`),
  KEY `idx_dict_type` (`dict_type`),
  KEY `idx_domain` (`domain`),
  KEY `idx_parent_code` (`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务字典';
