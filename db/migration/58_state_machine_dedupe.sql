SET NAMES utf8mb4;
-- =====================================================================
-- 58_state_machine_dedupe.sql  状态机规则去重 + 子状态列收紧
--
-- 根因：唯一键 uk_machine_from_event(machine_code, from_state,
-- from_sub_state, event_code) 含 from_sub_state，单维状态机该列全为
-- NULL，而 MySQL 唯一键对 NULL 不去重 —— 种子 state_machine_seed.sql 的
-- ODKU 幂等守卫永远不触发，种子每重跑一次即整份翻倍
-- （线上 92 行 = 46 条规则 × 2：id 1-46 为 08-05 原始，47/48 为 e2e
-- 遗留软删行，49-94 为 08-14 种子重跑副本）。
--
-- 引擎匹配只读 from_state + event（StateMachineEngine/StateRule 不含子
-- 状态列），服务层 checkUnique 已兼容 NULL 与 '' 两种取值，NULL → ''
-- 收紧无行为影响。
--
-- 修复：
--   1. 清理 e2e 测试软删遗留（占用唯一键位，防后续新增撞 DB 唯一键 500）；
--   2. 同逻辑键保留最小 id（原始种子行），物理删除重跑副本；
--   3. 子状态 NULL 归一为 ''；
--   4. 两列收紧为 NOT NULL DEFAULT '' —— 全新初始化下本迁移先于
--      99_seed.sh 执行，种子落 '' 后唯一键真正生效，ODKU 守卫自此可用。
-- 幂等：全部语句可重复执行（第二次运行为空操作）。
-- =====================================================================

-- 1. e2e 测试软删遗留清理
DELETE FROM `system_state_machine`
 WHERE `deleted` = 1 AND `event_code` IN ('e2e_test', 'e2e_browser');

-- 2. 重复副本清理：同 (machine_code, from_state, from_sub_state, event_code) 保留最小 id
DELETE t1 FROM `system_state_machine` t1
JOIN `system_state_machine` t2
  ON  t1.`machine_code` = t2.`machine_code`
  AND t1.`from_state` = t2.`from_state`
  AND IFNULL(t1.`from_sub_state`, '') = IFNULL(t2.`from_sub_state`, '')
  AND t1.`event_code` = t2.`event_code`
  AND t1.`id` > t2.`id`;

-- 3. 子状态 NULL → ''（含软删行，避免收紧列定义时遗留 NULL）
UPDATE `system_state_machine` SET `from_sub_state` = '' WHERE `from_sub_state` IS NULL;
UPDATE `system_state_machine` SET `to_sub_state` = '' WHERE `to_sub_state` IS NULL;

-- 4. 列收紧：单维状态机的空子状态以 '' 落库，唯一键对全部行生效
ALTER TABLE `system_state_machine`
  MODIFY `from_sub_state` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '源子状态值（单维状态机为空串）',
  MODIFY `to_sub_state` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '目标子状态值（单维状态机为空串）';
