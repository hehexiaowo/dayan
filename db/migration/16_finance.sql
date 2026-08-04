-- =====================================================================
-- 16_finance.sql  结算域（finance_）
-- 域说明：财务流水、结算单、发票、应收应付账目、对账记录、支付记录、退款记录
--         v4.2 调整：原 5 表扩展为 7 表，新增 finance_payment / finance_refund
--         （由订单域 order_payment / order_refund 迁入，统一资金往来管理）
-- 表数：7
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.16
-- 主键策略：全部为分片表（雪花ID，应用层 SnowflakeId）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.16.1 finance_flow 财务流水（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `finance_flow`;
CREATE TABLE `finance_flow` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `flow_code` VARCHAR(50) NOT NULL COMMENT '流水编号',
  `flow_type` TINYINT(2) NOT NULL COMMENT '流水类型（1=收入, 2=支出, 3=退款, 4=结算）',
  `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型（字符串枚举：equity_order=权益订单, scene_order=场景订单, course_order=课程订单, travel_order=旅居订单, settlement=结算）',
  `biz_code` VARCHAR(64) DEFAULT NULL COMMENT '业务编码',
  `account_type` VARCHAR(30) NOT NULL COMMENT '账号类型（organ/channel/agent/client/supplier）',
  `account_code` VARCHAR(50) NOT NULL COMMENT '账号编码',
  `flow_amount` DECIMAL(14,2) NOT NULL COMMENT '流水金额',
  `balance_before` DECIMAL(14,2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(14,2) NOT NULL COMMENT '变动后余额',
  `pay_type` TINYINT(2) DEFAULT NULL COMMENT '支付方式（pay_type 字典：1=微信支付, 2=支付宝, 3=银行转账, 4=余额支付, 5=线下支付）',
  `trade_no` VARCHAR(100) DEFAULT NULL COMMENT '交易流水号',
  `counterparty_type` VARCHAR(30) DEFAULT NULL COMMENT '对方类型',
  `counterparty_code` VARCHAR(50) DEFAULT NULL COMMENT '对方编码',
  `counterparty_name` VARCHAR(200) DEFAULT NULL COMMENT '对方名称',
  `flow_description` VARCHAR(500) DEFAULT NULL COMMENT '流水描述',
  `flow_time` DATETIME NOT NULL COMMENT '流水时间',
  `is_settled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已结算（0=否, 1=是）',
  `settle_code` VARCHAR(64) DEFAULT NULL COMMENT '结算单编码',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=已冲正, 1=正常）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_code` (`flow_code`),
  KEY `idx_flow_type` (`flow_type`),
  KEY `idx_biz` (`biz_type`, `biz_code`),
  KEY `idx_account` (`account_type`, `account_code`),
  KEY `idx_flow_time` (`flow_time`),
  KEY `idx_is_settled` (`is_settled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务流水';

-- ---------------------------------------------------------------------
-- 3.16.2 finance_bill 结算单（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `finance_bill`;
CREATE TABLE `finance_bill` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `bill_code` VARCHAR(50) NOT NULL COMMENT '结算单编号',
  `bill_type` TINYINT(2) NOT NULL COMMENT '结算类型（1=渠道结算, 2=供应商结算）',
  `target_type` VARCHAR(30) NOT NULL COMMENT '结算对象类型（channel/supplier/distributor）',
  `target_code` VARCHAR(50) NOT NULL COMMENT '结算对象编码',
  `target_name` VARCHAR(200) NOT NULL COMMENT '结算对象名称',
  `period_start` DATE NOT NULL COMMENT '结算周期开始',
  `period_end` DATE NOT NULL COMMENT '结算周期结束',
  `order_count` INT(11) NOT NULL DEFAULT 0 COMMENT '订单数量',
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT '结算总额',
  `commission_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '分销手续费金额',
  `refund_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '退款金额',
  `adjust_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '调整金额',
  `final_amount` DECIMAL(14,2) NOT NULL COMMENT '最终结算金额',
  `flow_ids` TEXT DEFAULT NULL COMMENT '关联流水ID列表（JSON数组）',
  `settlement_method` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '结算方式（1=银行转账, 2=线上转账）',
  `bank_info` VARCHAR(500) DEFAULT NULL COMMENT '收款银行信息',
  `apply_time` DATETIME DEFAULT NULL COMMENT '申请时间',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `settle_time` DATETIME DEFAULT NULL COMMENT '结算完成时间',
  `auditor_code` VARCHAR(64) DEFAULT NULL COMMENT '审核人编码',
  `auditor_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人姓名',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `bill_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待审核, 1=审核通过, 2=结算中, 3=已结算, 4=审核拒绝）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_code` (`bill_code`),
  KEY `idx_bill_type` (`bill_type`),
  KEY `idx_target` (`target_type`, `target_code`),
  KEY `idx_period` (`period_start`, `period_end`),
  KEY `idx_bill_status` (`bill_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算单';

-- ---------------------------------------------------------------------
-- 3.16.3 finance_invoice 发票（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `finance_invoice`;
CREATE TABLE `finance_invoice` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `invoice_code` VARCHAR(50) NOT NULL COMMENT '发票编码',
  `invoice_type` TINYINT(2) NOT NULL COMMENT '发票类型（1=增值税普通发票, 2=增值税专用发票, 3=电子发票）',
  `bill_code` VARCHAR(64) DEFAULT NULL COMMENT '关联结算单编码',
  `order_code` VARCHAR(64) DEFAULT NULL COMMENT '关联订单编码',
  `applicant_type` VARCHAR(30) NOT NULL COMMENT '申请方类型（channel/agent/client）',
  `applicant_code` VARCHAR(50) NOT NULL COMMENT '申请方编码',
  `applicant_name` VARCHAR(200) NOT NULL COMMENT '申请方名称',
  `title_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '抬头类型（1=企业, 2=个人）',
  `invoice_title` VARCHAR(200) NOT NULL COMMENT '发票抬头',
  `tax_no` VARCHAR(50) DEFAULT NULL COMMENT '纳税人识别号',
  `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号',
  `register_address` VARCHAR(500) DEFAULT NULL COMMENT '注册地址',
  `register_phone` VARCHAR(20) DEFAULT NULL COMMENT '注册电话',
  `invoice_amount` DECIMAL(14,2) NOT NULL COMMENT '开票金额',
  `invoice_content` VARCHAR(500) NOT NULL COMMENT '发票内容',
  `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '收件人姓名',
  `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收件人电话',
  `receiver_address` VARCHAR(500) DEFAULT NULL COMMENT '收件地址',
  `receiver_email` VARCHAR(100) DEFAULT NULL COMMENT '收件邮箱（电子发票）',
  `invoice_no` VARCHAR(50) DEFAULT NULL COMMENT '发票号码',
  `invoice_url` VARCHAR(500) DEFAULT NULL COMMENT '发票文件URL',
  `apply_time` DATETIME NOT NULL COMMENT '申请时间',
  `issue_time` DATETIME DEFAULT NULL COMMENT '开票时间',
  `send_time` DATETIME DEFAULT NULL COMMENT '寄出时间',
  `invoice_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待审核, 1=已审核, 2=已开票, 3=已寄出, 4=已完成, 5=已作废, 6=已红冲）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_code` (`invoice_code`),
  KEY `idx_bill_code` (`bill_code`),
  KEY `idx_applicant` (`applicant_type`, `applicant_code`),
  KEY `idx_invoice_no` (`invoice_no`),
  KEY `idx_invoice_status` (`invoice_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票';

-- ---------------------------------------------------------------------
-- 3.16.4 finance_account 应收应付账目（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `finance_account`;
CREATE TABLE `finance_account` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `account_code` VARCHAR(50) NOT NULL COMMENT '账目编码',
  `direction` TINYINT(2) NOT NULL COMMENT '账目方向（1=应收, 2=应付）',
  `account_type` VARCHAR(30) NOT NULL COMMENT '对象类型（channel/supplier/agent）',
  `target_code` VARCHAR(50) NOT NULL COMMENT '对象编码',
  `target_name` VARCHAR(200) NOT NULL COMMENT '对象名称',
  `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型（字符串枚举：equity_purchase=权益采购, scene_fee=场景费用, service_fee=服务费）',
  `biz_code` VARCHAR(64) DEFAULT NULL COMMENT '业务编码',
  `total_amount` DECIMAL(14,2) NOT NULL COMMENT '应收/应付总额',
  `received_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已收/已付金额',
  `remain_amount` DECIMAL(14,2) NOT NULL COMMENT '未收/未付金额',
  `due_date` DATE DEFAULT NULL COMMENT '到期日期',
  `last_receive_time` DATETIME DEFAULT NULL COMMENT '最近收款/付款时间',
  `account_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待收/付, 1=部分收/付, 2=已结清, 3=已逾期, 4=已坏账）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_code` (`account_code`),
  KEY `idx_direction` (`direction`),
  KEY `idx_account` (`account_type`, `target_code`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_account_status` (`account_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收应付账目';

-- ---------------------------------------------------------------------
-- 3.16.5 finance_reconciliation 对账记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `finance_reconciliation`;
CREATE TABLE `finance_reconciliation` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `recon_code` VARCHAR(50) NOT NULL COMMENT '对账编码',
  `recon_type` TINYINT(2) NOT NULL COMMENT '对账类型（1=渠道对账, 2=供应商对账）',
  `target_code` VARCHAR(50) NOT NULL COMMENT '对账对象编码',
  `target_name` VARCHAR(200) NOT NULL COMMENT '对账对象名称',
  `period_start` DATE NOT NULL COMMENT '对账周期开始',
  `period_end` DATE NOT NULL COMMENT '对账周期结束',
  `our_order_count` INT(11) NOT NULL DEFAULT 0 COMMENT '我方订单数',
  `our_total_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '我方总金额',
  `their_order_count` INT(11) DEFAULT NULL COMMENT '对方订单数',
  `their_total_amount` DECIMAL(14,2) DEFAULT NULL COMMENT '对方总金额',
  `diff_count` INT(11) NOT NULL DEFAULT 0 COMMENT '差异订单数',
  `diff_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '差异金额',
  `diff_detail` TEXT DEFAULT NULL COMMENT '差异明细（JSON格式）',
  `recon_result` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '对账结果（0=有差异, 1=一致）',
  `handle_result` TEXT DEFAULT NULL COMMENT '差异处理结果',
  `recon_time` DATETIME NOT NULL COMMENT '对账时间',
  `operator_code` VARCHAR(64) NOT NULL COMMENT '操作人编码',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=对账中, 1=已完成, 2=待确认, 3=已确认）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recon_code` (`recon_code`),
  KEY `idx_target_code` (`target_code`),
  KEY `idx_period` (`period_start`, `period_end`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账记录';

-- ---------------------------------------------------------------------
-- 3.16.6 finance_payment 订单支付记录（分片表，雪花ID）
-- v4.2 从订单域 order_payment 迁入结算域
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `finance_payment`;
CREATE TABLE `finance_payment` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `payment_code` VARCHAR(50) NOT NULL COMMENT '支付流水号',
  `order_type` TINYINT(2) NOT NULL COMMENT '订单类型（1=权益, 2=场景, 3=课程, 4=旅居）',
  `order_code` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `pay_type` TINYINT(2) NOT NULL COMMENT '支付方式（pay_type 字典：1=微信支付, 2=支付宝, 3=银行转账, 4=余额支付, 5=线下支付）',
  `pay_amount` DECIMAL(14,2) NOT NULL COMMENT '支付金额',
  `trade_no` VARCHAR(100) DEFAULT NULL COMMENT '第三方交易号',
  `payer_account` VARCHAR(100) DEFAULT NULL COMMENT '付款方账号',
  `payee_account` VARCHAR(100) DEFAULT NULL COMMENT '收款方账号',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `notify_time` DATETIME DEFAULT NULL COMMENT '回调通知时间',
  `pay_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '支付状态（0=待支付, 1=支付成功, 2=支付失败, 3=已退款, 4=部分退款）',
  `pay_description` VARCHAR(500) DEFAULT NULL COMMENT '支付说明',
  `extra_data` TEXT DEFAULT NULL COMMENT '扩展数据（JSON格式）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_code` (`payment_code`),
  KEY `idx_order` (`order_type`, `order_code`),
  KEY `idx_trade_no` (`trade_no`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_pay_time` (`pay_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单支付记录';

-- ---------------------------------------------------------------------
-- 3.16.7 finance_refund 订单退款记录（分片表，雪花ID）
-- v4.2 从订单域 order_refund 迁入结算域
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `finance_refund`;
CREATE TABLE `finance_refund` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `refund_code` VARCHAR(50) NOT NULL COMMENT '退款编码',
  `order_type` TINYINT(2) NOT NULL COMMENT '订单类型',
  `order_code` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `payment_code` VARCHAR(64) DEFAULT NULL COMMENT '原支付记录编码',
  `refund_amount` DECIMAL(14,2) NOT NULL COMMENT '退款金额',
  `refund_reason` VARCHAR(500) NOT NULL COMMENT '退款原因',
  `refund_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '退款类型（1=全额退款, 2=部分退款）',
  `refund_channel` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '退款渠道（1=原路退回, 2=退到余额, 3=线下退款）',
  `refund_trade_no` VARCHAR(100) DEFAULT NULL COMMENT '退款交易号',
  `apply_time` DATETIME NOT NULL COMMENT '申请时间',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `refund_time` DATETIME DEFAULT NULL COMMENT '退款完成时间',
  `auditor_code` VARCHAR(64) DEFAULT NULL COMMENT '审核人编码',
  `auditor_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人姓名',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `refund_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=待审核, 1=审核通过, 2=退款中, 3=退款成功, 4=审核拒绝, 5=退款失败）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_code` (`refund_code`),
  KEY `idx_order` (`order_type`, `order_code`),
  KEY `idx_refund_status` (`refund_status`),
  KEY `idx_apply_time` (`apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单退款记录';
