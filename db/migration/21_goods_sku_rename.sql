-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- ============================================================
-- 增量3：商品多态表重命名（goods_sku_scene/course/sojourn → goods_scene/course/sojourn）
-- 仅改表名，字段不变。goods_sku_equity 保留（增量5删除）。
-- ============================================================

RENAME TABLE `goods_sku_scene`   TO `goods_scene`;
RENAME TABLE `goods_sku_course`  TO `goods_course`;
RENAME TABLE `goods_sku_sojourn` TO `goods_sojourn`;
