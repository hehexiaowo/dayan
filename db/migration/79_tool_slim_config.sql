SET NAMES utf8mb4;
-- =====================================================================
-- 79_tool_slim_config.sql  工具配置表瘦身
--
-- tool_info 仅保留「定义 + 配置」：工具编码/名称/类型/简介/config_json/
-- 状态/备注。入口路径、图标、可见端、排序号移除——端上页面路径、图标、
-- 颜色等展示细节由前端按 tool_type 固定映射，类型化配置仍走 config_json；
-- 可见端不再配置，启用即全端可见（目前仅 agent 端有工具列表页）。
-- 历史迁移（42/74/76）不回改，新库按编号链执行同样落到最终结构。
-- =====================================================================

-- 排序索引随 sort_order 移除（status 区分度低，不再单独建索引）
ALTER TABLE `tool_info`
  DROP INDEX `idx_status_sort`;

ALTER TABLE `tool_info`
  DROP COLUMN `entry_path`,
  DROP COLUMN `icon`,
  DROP COLUMN `visible_scope`,
  DROP COLUMN `sort_order`;

ALTER TABLE `tool_info` COMMENT = '工具实例定义（平台共享表；展示/路由细节由端上按 tool_type 固定映射）';
