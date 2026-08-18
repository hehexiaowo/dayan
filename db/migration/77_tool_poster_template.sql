SET NAMES utf8mb4;
-- =====================================================================
-- 77_tool_poster_template.sql  海报模板归入工具域
-- poster_template → tool_poster_template（平台共享，前端浏览，不建记录表）
-- =====================================================================
RENAME TABLE `poster_template` TO `tool_poster_template`;

ALTER TABLE `tool_poster_template` COMMENT = '营销海报模板（工具域，平台共享）';
