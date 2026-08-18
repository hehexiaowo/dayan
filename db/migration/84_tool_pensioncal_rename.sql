SET NAMES utf8mb4;
-- =====================================================================
-- 84_tool_pensioncal_rename.sql  perncal → pensioncal
--
-- 承接 81：tool_perncal_record 收敛为 pensioncal（社保养老计算器
-- 缩写保留完整语义）。
-- =====================================================================

RENAME TABLE `tool_perncal_record` TO `tool_pensioncal_record`;

ALTER TABLE `tool_pensioncal_record` COMMENT = '社保养老计算器使用记录';
