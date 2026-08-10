-- ============================================================
-- 29_network_tags.sql
-- 机构网络归属字段：让 admin 精准配置每家机构属于哪些网络（活力长居/照护长居/旅居）
-- 一个机构可多选（如 vital,sojourn），替代原来靠 ability_type + stay_type 间接推导
-- ============================================================

SET NAMES utf8mb4;

-- 1. 加列
ALTER TABLE `park_info`
  ADD COLUMN `network_tags` VARCHAR(64) DEFAULT NULL
    COMMENT '网络标签（逗号分隔：vital=活力长居,care=照护长居,sojourn=旅居养老）'
    AFTER `ability_type_description`;

-- 2. 从 ability_type 回填 vital / care
UPDATE `park_info` SET `network_tags` = 'vital'
  WHERE `ability_type` = 1 AND `deleted` = 0;

UPDATE `park_info` SET `network_tags` = 'care'
  WHERE `ability_type` IN (2, 3, 4, 7) AND `deleted` = 0;

-- 3. 从 park_room_type.stay_type=2 追加 sojourn 标签（CONCAT_WS 自动处理 NULL → 仅 'sojourn'）
UPDATE `park_info` pi
INNER JOIN (
  SELECT DISTINCT `park_code` FROM `park_room_type`
  WHERE `stay_type` = 2 AND `deleted` = 0
) rt ON rt.`park_code` = pi.`park_code`
SET pi.`network_tags` = CONCAT_WS(',', pi.`network_tags`, 'sojourn')
WHERE pi.`deleted` = 0;
