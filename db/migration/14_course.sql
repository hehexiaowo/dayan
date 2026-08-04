-- =====================================================================
-- 14_course.sql  课程域（course_）
-- 域说明：课程信息、讲师、学员学习记录（支持线上录播/直播/线下/混合课程）
-- 表数：3
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.14
-- 主键策略：course_info / course_lecturer 为平台共享表（AUTO_INCREMENT），
--           course_record_learn 为分片表（雪花ID，应用层 SnowflakeId）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.14.1 course_info 课程信息（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `course_info`;
CREATE TABLE `course_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_code` VARCHAR(50) NOT NULL COMMENT '课程编码',
  `course_name` VARCHAR(200) NOT NULL COMMENT '课程名称',
  `course_type` TINYINT(2) NOT NULL COMMENT '课程类型（1=线上录播, 2=线上直播, 3=线下课程, 4=混合课程）',
  `category_code` VARCHAR(50) DEFAULT NULL COMMENT '分类编码',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `video_url` VARCHAR(500) DEFAULT NULL COMMENT '宣传视频URL',
  `course_description` TEXT DEFAULT NULL COMMENT '课程描述',
  `course_outline` TEXT DEFAULT NULL COMMENT '课程大纲（JSON格式）',
  `target_audience` VARCHAR(500) DEFAULT NULL COMMENT '目标人群',
  `learning_objectives` TEXT DEFAULT NULL COMMENT '学习目标',
  `lecturer_code` VARCHAR(64) DEFAULT NULL COMMENT '主讲讲师编码',
  `total_class` INT(11) NOT NULL DEFAULT 0 COMMENT '总课时数',
  `total_duration` INT(11) DEFAULT NULL COMMENT '总时长（分钟）',
  `valid_days` INT(11) DEFAULT NULL COMMENT '有效天数',
  `original_price` DECIMAL(12,2) NOT NULL COMMENT '原价',
  `sale_price` DECIMAL(12,2) NOT NULL COMMENT '售价',
  `max_students` INT(11) DEFAULT NULL COMMENT '最大学员数（线下/直播课）',
  `current_students` INT(11) NOT NULL DEFAULT 0 COMMENT '当前学员数',
  `view_count` INT(11) NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `sales_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已售数量',
  `rating_avg` DECIMAL(3,2) DEFAULT NULL COMMENT '平均评分',
  `is_free` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否免费（0=否, 1=是）',
  `is_recommend` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐',
  `course_start_date` DATE DEFAULT NULL COMMENT '开课日期',
  `course_end_date` DATE DEFAULT NULL COMMENT '结课日期',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `course_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=草稿, 1=待上架, 2=已上架, 3=已下架, 4=已结课）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_course_type` (`course_type`),
  KEY `idx_lecturer_code` (`lecturer_code`),
  KEY `idx_course_status` (`course_status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程信息';

-- ---------------------------------------------------------------------
-- 3.14.2 course_lecturer 课程讲师（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `course_lecturer`;
CREATE TABLE `course_lecturer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lecturer_code` VARCHAR(50) NOT NULL COMMENT '讲师编码',
  `lecturer_name` VARCHAR(50) NOT NULL COMMENT '讲师姓名',
  `gender` TINYINT(1) DEFAULT 0 COMMENT '性别（0=未知, 1=男, 2=女）',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '职称/头衔',
  `organization` VARCHAR(200) DEFAULT NULL COMMENT '所属机构',
  `specialty` VARCHAR(500) DEFAULT NULL COMMENT '擅长领域（JSON数组）',
  `introduction` TEXT DEFAULT NULL COMMENT '讲师简介',
  `certifications` TEXT DEFAULT NULL COMMENT '资质证书（JSON数组）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  `course_count` INT(11) NOT NULL DEFAULT 0 COMMENT '开课数量',
  `student_count` INT(11) NOT NULL DEFAULT 0 COMMENT '学员总数',
  `rating_avg` DECIMAL(3,2) DEFAULT NULL COMMENT '平均评分',
  `is_certified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否平台认证（0=否, 1=是）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lecturer_code` (`lecturer_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程讲师';

-- ---------------------------------------------------------------------
-- 3.14.3 course_record_learn 课程学习记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `course_record_learn`;
CREATE TABLE `course_record_learn` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `course_code` VARCHAR(50) NOT NULL COMMENT '课程编码',
  `client_code` VARCHAR(64) DEFAULT NULL COMMENT '学员客户编码',
  `agent_code` VARCHAR(64) DEFAULT NULL COMMENT '学员代理人编码',
  `learner_name` VARCHAR(50) NOT NULL COMMENT '学员姓名',
  `learner_phone` VARCHAR(20) DEFAULT NULL COMMENT '学员手机号',
  `enroll_time` DATETIME NOT NULL COMMENT '报名时间',
  `order_code` VARCHAR(64) DEFAULT NULL COMMENT '关联订单编码',
  `current_lesson` INT(11) NOT NULL DEFAULT 0 COMMENT '当前学到第几课',
  `total_lesson` INT(11) NOT NULL DEFAULT 0 COMMENT '总课时',
  `learn_progress` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '学习进度（%）',
  `total_learn_time` INT(11) NOT NULL DEFAULT 0 COMMENT '累计学习时长（分钟）',
  `last_learn_time` DATETIME DEFAULT NULL COMMENT '最近学习时间',
  `is_completed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成（0=否, 1=是）',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `certificate_url` VARCHAR(500) DEFAULT NULL COMMENT '结业证书URL',
  `rating` TINYINT(1) DEFAULT NULL COMMENT '课程评分（1-5）',
  `rating_content` VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=已退课, 1=学习中, 2=已完成, 3=已过期）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_code` (`course_code`),
  KEY `idx_client_code` (`client_code`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_status` (`status`),
  KEY `idx_is_completed` (`is_completed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程学习记录';
