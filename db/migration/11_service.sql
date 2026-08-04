-- =====================================================================
-- 11_service.sql  服务域（service_）
-- 域说明：服务会话、需求收集、方案定制、全程安排、回访品控、服务评价、探访记录
--         （养老管家为客户提供服务的核心流程域，7态主状态+子状态二维状态机）
-- 表数：7
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.11
-- 主键策略：全部为分片表（雪花ID，应用层 SnowflakeId）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.11.1 service_session 服务会话（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `service_session`;
CREATE TABLE `service_session` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `session_code` VARCHAR(50) NOT NULL COMMENT '会话编码',
  `equity_code` VARCHAR(50) DEFAULT NULL COMMENT '关联权益编码',
  `client_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `butler_code` VARCHAR(50) DEFAULT NULL COMMENT '服务管家编码（待分配时为空）',
  `butler_full_name` VARCHAR(50) DEFAULT NULL COMMENT '服务管家姓名（快照）',
  `service_type` TINYINT(2) NOT NULL COMMENT '服务类型（1=机构入住服务, 2=场景活动服务, 3=居家养老服务, 4=健康咨询服务）',
  `service_title` VARCHAR(200) DEFAULT NULL COMMENT '服务标题',
  `service_description` TEXT DEFAULT NULL COMMENT '服务描述',
  `priority` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '优先级（0=普通, 1=优先, 2=紧急, 3=非常紧急）',
  `source_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '来源（1=权益触发, 2=客户主动, 3=代理人委托, 4=管家发起）',
  `source_code` VARCHAR(64) DEFAULT NULL COMMENT '来源编码',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '关联养老机构编码',
  `park_full_name` VARCHAR(200) DEFAULT NULL COMMENT '关联养老机构名称（快照）',
  `agent_code` VARCHAR(64) DEFAULT NULL COMMENT '关联代理人编码',
  `channel_code` VARCHAR(50) DEFAULT NULL COMMENT '关联渠道编码',
  `accept_time` DATETIME DEFAULT NULL COMMENT '受理时间',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
  `total_duration` INT(11) DEFAULT NULL COMMENT '总服务时长（小时）',
  `touch_count` INT(11) NOT NULL DEFAULT 0 COMMENT '服务接触次数',
  `is_satisfied` TINYINT(1) DEFAULT NULL COMMENT '是否满意（0=否, 1=是）',
  `overall_rating` TINYINT(1) DEFAULT NULL COMMENT '综合评分（1-5）',
  `session_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '会话状态（1=待分配, 2=处理中, 3=方案待确认, 4=服务安排中, 5=服务中, 6=已完成, 7=已取消）',
  `sub_status` VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '子状态（normal/hold/urgent/reassign/refund_review/refund_done/interrupted）',
  `close_reason` VARCHAR(500) DEFAULT NULL COMMENT '关闭原因',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_code` (`session_code`),
  KEY `idx_equity_code` (`equity_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_service_type` (`service_type`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_session_status` (`session_status`),
  KEY `idx_sub_status` (`sub_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务会话';

-- ---------------------------------------------------------------------
-- 3.11.2 service_equity_demand 权益服务-需求收集（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `service_equity_demand`;
CREATE TABLE `service_equity_demand` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `session_code` VARCHAR(50) NOT NULL COMMENT '服务会话编码',
  `client_code` VARCHAR(64) NOT NULL COMMENT '客户编码',
  `butler_code` VARCHAR(64) NOT NULL COMMENT '管家编码',
  `demand_type` TINYINT(2) NOT NULL COMMENT '需求类型（1=机构入住需求, 2=日间照料需求, 3=居家护理需求, 4=场景活动需求, 5=旅居需求）',
  `use_person_name` VARCHAR(50) DEFAULT NULL COMMENT '使用人姓名',
  `use_person_age` TINYINT(3) DEFAULT NULL COMMENT '使用人年龄',
  `use_person_gender` TINYINT(1) DEFAULT NULL COMMENT '使用人性别',
  `health_summary` TEXT DEFAULT NULL COMMENT '健康状况概述',
  `care_level_need` TINYINT(2) DEFAULT NULL COMMENT '所需照护等级',
  `city_preference` VARCHAR(200) DEFAULT NULL COMMENT '城市偏好（JSON数组）',
  `area_preference` VARCHAR(200) DEFAULT NULL COMMENT '区域偏好（JSON数组）',
  `budget_min` DECIMAL(12,2) DEFAULT NULL COMMENT '预算下限',
  `budget_max` DECIMAL(12,2) DEFAULT NULL COMMENT '预算上限',
  `room_preference` VARCHAR(200) DEFAULT NULL COMMENT '房间偏好（JSON数组）',
  `food_preference` VARCHAR(200) DEFAULT NULL COMMENT '饮食偏好',
  `special_needs` TEXT DEFAULT NULL COMMENT '特殊需求',
  `expected_time` DATE DEFAULT NULL COMMENT '期望服务时间',
  `contact_preference` TINYINT(2) DEFAULT NULL COMMENT '联系偏好（1=电话, 2=微信, 3=短信）',
  `collect_method` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '收集方式（1=电话沟通, 2=上门拜访, 3=在线填写, 4=代理人转述）',
  `collect_time` DATETIME NOT NULL COMMENT '收集时间',
  `demand_summary` TEXT DEFAULT NULL COMMENT '需求总结',
  `demand_images` TEXT DEFAULT NULL COMMENT '需求相关资料图片（JSON数组）',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待处理, 1=已整理, 2=已确认）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_code` (`session_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_demand_type` (`demand_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益服务-需求收集';

-- ---------------------------------------------------------------------
-- 3.11.3 service_equity_solution 权益服务-方案定制（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `service_equity_solution`;
CREATE TABLE `service_equity_solution` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `session_code` VARCHAR(50) NOT NULL COMMENT '服务会话编码',
  `demand_code` VARCHAR(64) NOT NULL COMMENT '关联需求编码',
  `client_code` VARCHAR(64) NOT NULL COMMENT '客户编码',
  `butler_code` VARCHAR(64) NOT NULL COMMENT '管家编码',
  `solution_code` VARCHAR(50) NOT NULL COMMENT '方案编码',
  `solution_name` VARCHAR(200) NOT NULL COMMENT '方案名称',
  `solution_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '方案类型（1=推荐方案, 2=备选方案）',
  `recommended_parks` TEXT DEFAULT NULL COMMENT '推荐机构列表（JSON数组，含机构ID、名称、推荐理由）',
  `plan_summary` TEXT DEFAULT NULL COMMENT '方案概述',
  `service_items` TEXT DEFAULT NULL COMMENT '服务项目明细（JSON数组）',
  `estimated_cost` DECIMAL(12,2) DEFAULT NULL COMMENT '预估费用',
  `cost_breakdown` TEXT DEFAULT NULL COMMENT '费用明细（JSON格式）',
  `timeline` TEXT DEFAULT NULL COMMENT '服务时间安排',
  `advantages` TEXT DEFAULT NULL COMMENT '方案优势',
  `risks` TEXT DEFAULT NULL COMMENT '注意事项/风险提示',
  `comparison` TEXT DEFAULT NULL COMMENT '与备选方案对比说明',
  `presentation_time` DATETIME DEFAULT NULL COMMENT '方案呈现时间',
  `presentation_method` TINYINT(2) DEFAULT NULL COMMENT '呈现方式（1=当面, 2=电话, 3=文档发送）',
  `client_feedback` TEXT DEFAULT NULL COMMENT '客户反馈',
  `is_accepted` TINYINT(1) DEFAULT NULL COMMENT '客户是否接受（0=否, 1=是, 2=需调整）',
  `adjust_count` INT(11) NOT NULL DEFAULT 0 COMMENT '调整次数',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=制定中, 1=待呈现, 2=已呈现, 3=已确认, 4=已拒绝, 5=需调整）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_solution_code` (`solution_code`),
  KEY `idx_session_code` (`session_code`),
  KEY `idx_demand_code` (`demand_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益服务-方案定制';

-- ---------------------------------------------------------------------
-- 3.11.4 service_equity_arrange 权益服务-全程安排（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `service_equity_arrange`;
CREATE TABLE `service_equity_arrange` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `session_code` VARCHAR(50) NOT NULL COMMENT '服务会话编码',
  `solution_code` VARCHAR(64) DEFAULT NULL COMMENT '关联方案编码',
  `client_code` VARCHAR(64) NOT NULL COMMENT '客户编码',
  `butler_code` VARCHAR(64) NOT NULL COMMENT '管家编码',
  `arrange_code` VARCHAR(50) NOT NULL COMMENT '安排编码',
  `arrange_type` TINYINT(2) NOT NULL COMMENT '安排类型（1=参观预约, 2=入住安排, 3=活动报名, 4=服务预约, 5=交通安排, 6=其他）',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '关联养老机构编码',
  `park_full_name` VARCHAR(200) DEFAULT NULL COMMENT '关联养老机构名称（快照）',
  `arrange_date` DATE DEFAULT NULL COMMENT '安排日期',
  `arrange_time_start` TIME DEFAULT NULL COMMENT '开始时间',
  `arrange_time_end` TIME DEFAULT NULL COMMENT '结束时间',
  `arrange_address` VARCHAR(500) DEFAULT NULL COMMENT '安排地址',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '对接联系人',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '对接联系电话',
  `participant_count` INT(11) DEFAULT NULL COMMENT '参与人数',
  `prepare_items` TEXT DEFAULT NULL COMMENT '准备事项（JSON数组）',
  `progress_notes` TEXT DEFAULT NULL COMMENT '进展备注',
  `confirm_time` DATETIME DEFAULT NULL COMMENT '客户确认时间',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `is_confirmed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已确认（0=否, 1=是）',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待安排, 1=已安排, 2=进行中, 3=已完成, 4=已取消）',
  `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_arrange_code` (`arrange_code`),
  KEY `idx_session_code` (`session_code`),
  KEY `idx_solution_code` (`solution_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_arrange_date` (`arrange_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益服务-全程安排';

-- ---------------------------------------------------------------------
-- 3.11.5 service_equity_followup 权益服务-回访品控（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `service_equity_followup`;
CREATE TABLE `service_equity_followup` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `session_code` VARCHAR(50) NOT NULL COMMENT '服务会话编码',
  `arrange_code` VARCHAR(64) DEFAULT NULL COMMENT '关联安排编码',
  `client_code` VARCHAR(64) NOT NULL COMMENT '客户编码',
  `butler_code` VARCHAR(64) NOT NULL COMMENT '回访管家编码',
  `followup_code` VARCHAR(50) NOT NULL COMMENT '回访编码',
  `followup_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '回访类型（1=服务后回访, 2=入住后回访, 3=定期回访, 4=投诉回访）',
  `followup_method` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '回访方式（1=电话, 2=微信, 3=上门, 4=问卷）',
  `followup_date` DATE NOT NULL COMMENT '回访日期',
  `followup_time` DATETIME DEFAULT NULL COMMENT '回访时间',
  `service_satisfaction` TINYINT(1) DEFAULT NULL COMMENT '服务满意度（1-5）',
  `park_satisfaction` TINYINT(1) DEFAULT NULL COMMENT '机构满意度（1-5）',
  `butler_satisfaction` TINYINT(1) DEFAULT NULL COMMENT '管家满意度（1-5）',
  `overall_satisfaction` TINYINT(1) DEFAULT NULL COMMENT '综合满意度（1-5）',
  `service_evaluation` TEXT DEFAULT NULL COMMENT '服务评价内容',
  `improvement_suggestions` TEXT DEFAULT NULL COMMENT '改进建议',
  `complaints` TEXT DEFAULT NULL COMMENT '投诉内容',
  `complaint_handle` TEXT DEFAULT NULL COMMENT '投诉处理情况',
  `is_followup_needed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要后续跟进（0=否, 1=是）',
  `followup_plan` TEXT DEFAULT NULL COMMENT '后续跟进计划',
  `next_followup_date` DATE DEFAULT NULL COMMENT '下次回访日期',
  `is_resolved` TINYINT(1) DEFAULT NULL COMMENT '问题是否已解决（0=否, 1=是）',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待回访, 1=回访中, 2=已完成, 3=需再跟进）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_followup_code` (`followup_code`),
  KEY `idx_session_code` (`session_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_followup_date` (`followup_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益服务-回访品控';

-- ---------------------------------------------------------------------
-- 3.11.6 service_evaluation 服务评价（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `service_evaluation`;
CREATE TABLE `service_evaluation` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `session_code` VARCHAR(64) NOT NULL COMMENT '服务会话编码',
  `client_code` VARCHAR(64) NOT NULL COMMENT '客户编码',
  `butler_code` VARCHAR(64) NOT NULL COMMENT '管家编码',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '关联机构编码',
  `attitude_rating` TINYINT(1) NOT NULL COMMENT '服务态度评分（1-5）',
  `professional_rating` TINYINT(1) DEFAULT NULL COMMENT '专业度评分（1-5）',
  `responsiveness_rating` TINYINT(1) DEFAULT NULL COMMENT '响应速度评分（1-5）',
  `satisfaction_rating` TINYINT(1) DEFAULT NULL COMMENT '满意度评分（1-5）',
  `content` TEXT DEFAULT NULL COMMENT '评价内容',
  `image_urls` TEXT DEFAULT NULL COMMENT '评价图片（JSON数组）',
  `is_anonymous` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `reply_content` TEXT DEFAULT NULL COMMENT '回复内容',
  `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
  `reply_by_code` VARCHAR(64) DEFAULT NULL COMMENT '回复人编码',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=已隐藏, 1=正常）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_code` (`session_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_butler_code` (`butler_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务评价';

-- ---------------------------------------------------------------------
-- 3.11.7 service_visit_record 服务探访记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `service_visit_record`;
CREATE TABLE `service_visit_record` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `butler_code` VARCHAR(50) NOT NULL COMMENT '管家编码',
  `park_code` VARCHAR(50) NOT NULL COMMENT '机构编码',
  `visit_date` DATE NOT NULL COMMENT '探访日期',
  `visit_purpose` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '探访目的（1=常规探访, 2=质量检查, 3=新机构考察, 4=问题核实）',
  `facility_check` TEXT DEFAULT NULL COMMENT '设施检查情况',
  `service_check` TEXT DEFAULT NULL COMMENT '服务检查情况',
  `hygiene_check` TEXT DEFAULT NULL COMMENT '卫生检查情况',
  `food_check` TEXT DEFAULT NULL COMMENT '餐饮检查情况',
  `safety_check` TEXT DEFAULT NULL COMMENT '安全检查情况',
  `overall_score` DECIMAL(5,2) DEFAULT NULL COMMENT '综合评分（0-100）',
  `issues_found` TEXT DEFAULT NULL COMMENT '发现问题',
  `improvement_suggestions` TEXT DEFAULT NULL COMMENT '改进建议',
  `images` TEXT DEFAULT NULL COMMENT '探访照片（JSON数组）',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=草稿, 1=已提交）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_butler_code` (`butler_code`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_visit_date` (`visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务探访记录';
