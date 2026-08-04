-- =====================================================================
-- 17_distributor.sql  分销商域（distributor_）
-- 域说明：分销商基础信息（支持企业/个人主体，记录资质/联系方式/银行账户）
--         分销商通过关联渠道参与业务推广；本域仅维护基础信息，不含账号登录
-- 表数：1
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.17
-- 主键策略：平台共享表（AUTO_INCREMENT）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.17.1 distributor_info 分销商信息（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `distributor_info`;
CREATE TABLE `distributor_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `distributor_code` VARCHAR(50) NOT NULL COMMENT '分销商编码（DS+5位数字）',
  `full_name` VARCHAR(200) NOT NULL COMMENT '分销商全称（企业全称或个人姓名）',
  `short_name` VARCHAR(50) DEFAULT NULL COMMENT '简称',
  `subject_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '主体类型（1=企业, 2=个人）',
  `unified_credit_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码（企业）',
  `legal_person` VARCHAR(50) DEFAULT NULL COMMENT '法定代表人（企业）',
  `business_license_no` VARCHAR(100) DEFAULT NULL COMMENT '营业执照号（企业）',
  `registered_capital` DECIMAL(12,2) DEFAULT NULL COMMENT '注册资本（企业）',
  `establish_date` DATE DEFAULT NULL COMMENT '成立日期（企业）',
  `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号（个人，加密存储）',
  `gender` TINYINT(1) DEFAULT 0 COMMENT '性别（个人，0=未知, 1=男, 2=女）',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  `province_code` VARCHAR(20) DEFAULT NULL COMMENT '省份编码',
  `city_code` VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
  `district_code` VARCHAR(20) DEFAULT NULL COMMENT '区划编码',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号（加密存储）',
  `bank_account_name` VARCHAR(100) DEFAULT NULL COMMENT '银行户名',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待审核, 1=已合作, 2=已暂停, 3=已终止）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_distributor_code` (`distributor_code`),
  KEY `idx_subject_type` (`subject_type`),
  KEY `idx_unified_credit_code` (`unified_credit_code`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分销商信息';
