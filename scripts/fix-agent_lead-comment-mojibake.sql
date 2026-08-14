-- =================================================================
-- fix-agent_lead-comment-mojibake.sql
-- 修复 agent_lead 表 COMMENT 的 cp1252 双重编码乱码。
-- 
-- 生成器: scripts/gen_fix_comment_mojibake.py
-- 修复原理: latin1(=cp1252) 双重编码逆向还原；字节丢失列用源文件值覆盖
-- 安全性: 仅改 COMMENT；列定义取自 information_schema 现状，不动其它属性。
--         执行前请先 review；本操作对正确 COMMENT 有破坏性，仅限乱码表。
-- =================================================================
SET NAMES utf8mb4;

-- agent_lead
ALTER TABLE `agent_lead`
  MODIFY COLUMN `id` bigint NOT NULL COMMENT '主键（雪花ID）',
  MODIFY COLUMN `lead_code` varchar(50) NOT NULL COMMENT '线索编码（LD+日期+序号，渠道内唯一）',
  MODIFY COLUMN `agent_code` varchar(50) NOT NULL COMMENT '归属代理人编码',
  MODIFY COLUMN `channel_code` varchar(50) NOT NULL COMMENT '所属渠道编码',
  MODIFY COLUMN `name` varchar(100) NULL DEFAULT NULL COMMENT '线索姓名（可能只是称呼）',
  MODIFY COLUMN `phone` varchar(20) NULL DEFAULT NULL COMMENT '联系电话',
  MODIFY COLUMN `gender` tinyint(1) NOT NULL DEFAULT 0 COMMENT '性别（0=未知, 1=男, 2=女）',
  MODIFY COLUMN `age` int NULL DEFAULT NULL COMMENT '年龄（可为空）',
  MODIFY COLUMN `lead_status` tinyint NOT NULL DEFAULT 1 COMMENT '线索状态（1=新线索, 2=跟进中, 3=意向, 4=已转化, 5=已流失）',
  MODIFY COLUMN `source_type` tinyint NOT NULL DEFAULT 1 COMMENT '来源类型（1=手工录入, 2=分享扫码, 3=活动接触, 4=转介绍, 5=内容引流）',
  MODIFY COLUMN `source_ref` varchar(100) NULL DEFAULT NULL COMMENT '来源溯源（share_code/activity_code/referrer 等）',
  MODIFY COLUMN `visitor_token` varchar(64) NULL DEFAULT NULL COMMENT '访客令牌（匿名唯一标识，UUID）',
  MODIFY COLUMN `visitor_source` varchar(20) NULL DEFAULT NULL COMMENT '访客来源（wechat/browser/unknown）',
  MODIFY COLUMN `wx_nickname` varchar(100) NULL DEFAULT NULL COMMENT '微信昵称',
  MODIFY COLUMN `wx_avatar` varchar(500) NULL DEFAULT NULL COMMENT '微信头像URL',
  MODIFY COLUMN `last_trace_time` datetime NULL DEFAULT NULL COMMENT '最后互动时间',
  MODIFY COLUMN `trace_count` int NOT NULL DEFAULT 0 COMMENT '互动总次数',
  MODIFY COLUMN `intention_level` tinyint(1) NULL DEFAULT NULL COMMENT '意向等级（1=低, 2=中, 3=高）',
  MODIFY COLUMN `interest_type` varchar(200) NULL DEFAULT NULL COMMENT '关注养老类型（旅居/活力长居/照护，逗号分隔）',
  MODIFY COLUMN `region` varchar(200) NULL DEFAULT NULL COMMENT '关注区域',
  MODIFY COLUMN `last_follow_time` datetime NULL DEFAULT NULL COMMENT '最后跟进时间',
  MODIFY COLUMN `converted_client_code` varchar(50) NULL DEFAULT NULL COMMENT '转化后的客户编码（关联 client_info.client_code）',
  MODIFY COLUMN `converted_at` datetime NULL DEFAULT NULL COMMENT '转化时间',
  MODIFY COLUMN `remark` varchar(500) NULL DEFAULT NULL COMMENT '备注',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  MODIFY COLUMN `creator` varchar(64) NULL DEFAULT 'system' COMMENT '创建人',
  MODIFY COLUMN `updater` varchar(64) NULL DEFAULT 'system' COMMENT '更新人',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  MODIFY COLUMN `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  MODIFY COLUMN `last_trace_type` int NULL DEFAULT NULL COMMENT '最后互动类型（1=内容 2=工具 3=海报）',
  COMMENT = '代理人线索';

