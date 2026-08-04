-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- admin_seed.sql  超级管理员种子数据
-- 包含：大雁养老公司 + 超级管理员账号（admin/admin123，BCrypt 哈希）
-- 生成依据：docs/02 §3.2.1 organ_info / §3.2.2 organ_account
-- 密码：admin123 的 BCrypt(strength=10) 哈希，可由 PasswordService.matches 校验
-- =====================================================================

-- 公司：大雁养老（运营方）
INSERT INTO `organ_info`
  (`id`, `organ_code`, `full_name`, `short_name`, `organ_type`, `unified_credit_code`,
   `legal_person`, `status`, `sort_order`, `description`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  (1, 'OR00001', '大雁养老服务有限公司', '大雁养老', 1, '91110000MA00DAYAN01',
   '管理员', 1, 0, '大雁养老服务权益平台运营方', NOW(), NOW(), 'system', 'system', 0);

-- 超级管理员账号：admin / admin123
-- BCrypt 哈希值由 PasswordService.encode("admin123") 生成，strength=10
INSERT INTO `organ_account`
  (`id`, `organ_code`, `account_code`, `username`, `password`, `salt`,
   `real_name`, `gender`, `phone`, `email`,
   `login_count`, `account_status`, `is_admin`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  (1, 'OR00001', 'AC00001', 'admin',
   '$2a$10$EzzipqnCA/fipsaUkRUjvurXNZ8vt5EeDHi6DEvCfEFYF5O1Kr/aq',
   'bcrypt-self-contained',
   '超级管理员', 0, '13800000000', 'admin@dayanpeng.com',
   0, 1, 1, '系统预置超级管理员，不可删除',
   NOW(), NOW(), 'system', 'system', 0);

-- 超级管理员角色（系统预置，拥有全部权限）
INSERT INTO `organ_role`
  (`id`, `organ_code`, `role_code`, `role_name`, `role_type`, `description`, `data_scope`, `status`, `sort_order`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  (1, 'OR00001', 'ROLE_SUPER_ADMIN', '超级管理员', 1, '系统预置超级管理员，拥有全部权限', 1, 1, 0,
   NOW(), NOW(), 'system', 'system', 0);
