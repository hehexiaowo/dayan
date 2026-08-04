-- =====================================================================
-- 10_equity.sql  权益卡/函域（equity_）
-- 域说明：权益模板、批次、卡/函库存、激活、使用人、更换权益人（连接保险公司与客户的核心商业载体）
-- 表数：6
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.10
-- 主键策略：equity_template 为平台共享表（AUTO_INCREMENT）；其余 5 表为分片表（雪花ID，应用层 SnowflakeId）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.10.1 equity_template 权益模板（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `equity_template`;
CREATE TABLE `equity_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
  `template_name` VARCHAR(200) NOT NULL COMMENT '模板名称（如"金色晚年·机构体验权益"）',
  `equity_type` TINYINT(2) NOT NULL COMMENT '权益类型（1=机构入住权益, 2=机构参观权益, 3=场景活动权益, 4=居家护理权益, 5=健康检测权益, 6=课程学习权益, 7=旅居体验权益）',
  `equity_level` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '权益等级（1=基础, 2=标准, 3=高级, 4=尊享, 5=定制）',
  `equity_value` DECIMAL(12,2) NOT NULL COMMENT '权益面值（元）',
  `cost_price` DECIMAL(12,2) NOT NULL COMMENT '成本价（渠道采购价）',
  `content_description` TEXT DEFAULT NULL COMMENT '权益内容描述',
  `service_items` TEXT DEFAULT NULL COMMENT '包含服务项目（JSON数组）',
  `applicable_parks` TEXT DEFAULT NULL COMMENT '适用机构范围（JSON数组，NULL=全部）',
  `applicable_cities` TEXT DEFAULT NULL COMMENT '适用城市范围（JSON数组，NULL=全部）',
  `valid_days` INT(11) NOT NULL DEFAULT 365 COMMENT '激活后有效天数',
  `shelf_life_days` INT(11) NOT NULL DEFAULT 730 COMMENT '库存有效期天数（未激活时）',
  `is_transferable` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可转让（0=否, 1=是）',
  `is_stackable` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可叠加使用（0=否, 1=是）',
  `max_use_count` INT(11) NOT NULL DEFAULT 1 COMMENT '最大使用次数',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '权益封面图',
  `card_design_url` VARCHAR(500) DEFAULT NULL COMMENT '卡面设计图URL',
  `terms` TEXT DEFAULT NULL COMMENT '使用说明/条款',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=停用, 1=启用, 2=已下架）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_equity_type` (`equity_type`),
  KEY `idx_equity_level` (`equity_level`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益模板';

-- ---------------------------------------------------------------------
-- 3.10.2 equity_batch 权益批次管理（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `equity_batch`;
CREATE TABLE `equity_batch` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `batch_code` VARCHAR(50) NOT NULL COMMENT '批次编码',
  `batch_name` VARCHAR(200) NOT NULL COMMENT '批次名称',
  `template_code` VARCHAR(50) NOT NULL COMMENT '权益模板编码',
  `channel_code` VARCHAR(50) DEFAULT NULL COMMENT '分配渠道编码（NULL=未分配）',
  `total_quantity` INT(11) NOT NULL COMMENT '总数量',
  `produced_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已生成数量',
  `allocated_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已分配数量',
  `outbound_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已出库数量',
  `activated_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已激活数量',
  `used_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已使用数量',
  `expired_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已过期数量',
  `voided_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已作废数量',
  `remain_count` INT(11) NOT NULL DEFAULT 0 COMMENT '剩余可用数量',
  `unit_cost` DECIMAL(12,2) NOT NULL COMMENT '单位成本',
  `total_cost` DECIMAL(14,2) NOT NULL COMMENT '批次总成本',
  `produce_date` DATE NOT NULL COMMENT '生产日期',
  `expire_date` DATE NOT NULL COMMENT '批次有效期',
  `batch_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '批次状态（0=待生产, 1=生产中, 2=已完成, 3=已出库, 4=已关闭）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_code` (`batch_code`),
  KEY `idx_template_code` (`template_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_batch_status` (`batch_status`),
  KEY `idx_expire_date` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益批次管理';

-- ---------------------------------------------------------------------
-- 3.10.3 equity_depot 权益卡/函库（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `equity_depot`;
CREATE TABLE `equity_depot` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `equity_code` VARCHAR(50) NOT NULL COMMENT '权益编码（EQ+12位数字）',
  `equity_no` VARCHAR(50) NOT NULL COMMENT '权益卡号（对外展示用）',
  `template_code` VARCHAR(50) NOT NULL COMMENT '权益模板编码',
  `batch_code` VARCHAR(50) NOT NULL COMMENT '批次编码',
  `equity_type` TINYINT(2) NOT NULL COMMENT '权益类型（冗余，便于查询）',
  `equity_value` DECIMAL(12,2) NOT NULL COMMENT '权益面值',
  `cost_price` DECIMAL(12,2) NOT NULL COMMENT '成本价',
  `channel_code` VARCHAR(50) DEFAULT NULL COMMENT '分配渠道编码',
  `agent_code` VARCHAR(50) DEFAULT NULL COMMENT '分配代理人编码',
  `client_code` VARCHAR(50) DEFAULT NULL COMMENT '领取客户编码',
  `produce_time` DATETIME NOT NULL COMMENT '入库时间',
  `allocate_time` DATETIME DEFAULT NULL COMMENT '分配时间',
  `outbound_channel_code` VARCHAR(50) DEFAULT NULL COMMENT '出库寄送渠道编码（出库时填写，记录寄送给哪个渠道）',
  `outbound_agent_code` VARCHAR(50) DEFAULT NULL COMMENT '出库寄送代理人编码（出库时填写，记录寄送给哪个代理人）',
  `outbound_time` DATETIME DEFAULT NULL COMMENT '出库时间',
  `logistics_no` VARCHAR(100) DEFAULT NULL COMMENT '物流单号（出库寄送物流追踪号）',
  `activate_time` DATETIME DEFAULT NULL COMMENT '激活时间',
  `first_use_time` DATETIME DEFAULT NULL COMMENT '首次使用时间',
  `last_use_time` DATETIME DEFAULT NULL COMMENT '最近使用时间',
  `use_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `max_use_count` INT(11) NOT NULL DEFAULT 1 COMMENT '最大使用次数',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（激活后计算）',
  `shelf_expire_time` DATETIME NOT NULL COMMENT '库存过期时间（未激活有效期）',
  `card_secret` VARCHAR(200) DEFAULT NULL COMMENT '卡密（加密存储，激活验证用）',
  `carrier_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '载体类型（1=权益卡, 2=权益函）',
  `activate_code` VARCHAR(20) DEFAULT NULL COMMENT '激活码（DY-8位，权益卡专用）',
  `bind_code` VARCHAR(20) DEFAULT NULL COMMENT '绑定码（BF-12位，权益函专用）',
  `qr_code_url` VARCHAR(500) DEFAULT NULL COMMENT '权益二维码URL',
  `order_code` VARCHAR(64) DEFAULT NULL COMMENT '关联订单编码',
  `equity_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '权益状态（0=库存中, 1=已出库, 2=已激活, 3=使用中, 4=已完成, 5=已过期, 6=已作废, 7=更换权益人中）',
  `void_reason` VARCHAR(500) DEFAULT NULL COMMENT '作废原因',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equity_code` (`equity_code`),
  UNIQUE KEY `uk_equity_no` (`equity_no`),
  KEY `idx_template_code` (`template_code`),
  KEY `idx_batch_code` (`batch_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_equity_status` (`equity_status`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_order_code` (`order_code`),
  KEY `idx_carrier_type` (`carrier_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益卡/函库';

-- ---------------------------------------------------------------------
-- 3.10.4 equity_activate 权益激活记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `equity_activate`;
CREATE TABLE `equity_activate` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `activate_code` VARCHAR(50) NOT NULL COMMENT '激活记录编码',
  `equity_code` VARCHAR(50) NOT NULL COMMENT '权益编码',
  `template_code` VARCHAR(64) NOT NULL COMMENT '权益模板编码',
  `client_code` VARCHAR(50) NOT NULL COMMENT '激活客户编码',
  `client_full_name` VARCHAR(50) NOT NULL COMMENT '激活客户姓名（快照）',
  `client_phone` VARCHAR(20) NOT NULL COMMENT '激活客户手机号',
  `activate_channel` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '激活渠道（1=APP, 2=小程序, 3=H5, 4=管家代激活, 5=代理人代激活）',
  `activate_source_code` VARCHAR(64) DEFAULT NULL COMMENT '激活来源编码（如代理人编码）',
  `activate_time` DATETIME NOT NULL COMMENT '激活时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `is_id_card_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否实名认证（0=否, 1=是）',
  `is_agreement_signed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否签署协议（0=否, 1=是）',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '激活IP',
  `device_info` VARCHAR(500) DEFAULT NULL COMMENT '设备信息',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activate_code` (`activate_code`),
  KEY `idx_equity_code` (`equity_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_activate_time` (`activate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益激活记录';

-- ---------------------------------------------------------------------
-- 3.10.5 equity_use_person 权益使用人（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `equity_use_person`;
CREATE TABLE `equity_use_person` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `equity_code` VARCHAR(50) NOT NULL COMMENT '权益编码',
  `client_code` VARCHAR(50) NOT NULL COMMENT '权益持有人编码（激活权益的客户）',
  `use_person_name` VARCHAR(50) NOT NULL COMMENT '使用人姓名',
  `use_person_gender` TINYINT(1) DEFAULT 0 COMMENT '使用人性别（0=未知, 1=男, 2=女）',
  `use_person_birthday` DATE DEFAULT NULL COMMENT '使用人出生日期',
  `use_person_age` TINYINT(3) DEFAULT NULL COMMENT '使用人年龄',
  `use_person_phone` VARCHAR(20) DEFAULT NULL COMMENT '使用人手机号',
  `use_person_id_card` VARCHAR(20) DEFAULT NULL COMMENT '使用人身份证号（加密存储）',
  `relation_with_holder` VARCHAR(20) NOT NULL COMMENT '与持有人关系（本人/配偶/子女/父母/其他）',
  `health_status` TEXT DEFAULT NULL COMMENT '健康状况简述',
  `care_need` TEXT DEFAULT NULL COMMENT '照护需求简述',
  `is_default_holder` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否默认权益人（0=否, 1=是；一张权益仅一人为默认权益人，可被 equity_change_holder 更换）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_equity_code` (`equity_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_is_default_holder` (`is_default_holder`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益使用人';

-- ---------------------------------------------------------------------
-- 3.10.6 equity_change_holder 更换权益人记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `equity_change_holder`;
CREATE TABLE `equity_change_holder` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `equity_code` VARCHAR(50) NOT NULL COMMENT '权益编码',
  `old_use_person_code` VARCHAR(64) DEFAULT NULL COMMENT '原权益使用人编码',
  `old_person_name` VARCHAR(50) NOT NULL COMMENT '原权益人姓名',
  `old_person_id_card` VARCHAR(20) DEFAULT NULL COMMENT '原权益人身份证号（加密存储）',
  `new_use_person_code` VARCHAR(64) DEFAULT NULL COMMENT '新权益使用人编码',
  `new_person_name` VARCHAR(50) NOT NULL COMMENT '新权益人姓名',
  `new_person_id_card` VARCHAR(20) DEFAULT NULL COMMENT '新权益人身份证号（加密存储）',
  `change_reason` VARCHAR(500) DEFAULT NULL COMMENT '更换原因',
  `change_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '更换状态（0=待处理, 1=已完成, 2=已回滚）',
  `operate_time` DATETIME NOT NULL COMMENT '操作时间',
  `operator_code` VARCHAR(64) NOT NULL COMMENT '操作人编码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_equity_code` (`equity_code`),
  KEY `idx_change_status` (`change_status`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='更换权益人记录';
