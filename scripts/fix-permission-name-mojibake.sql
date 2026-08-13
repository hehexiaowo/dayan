-- 修复 organ_permission 接口权限名双重编码乱码（7 行）
-- 乱码成因：UTF-8 字节被按 cp1252 解读后再次以 UTF-8 存储
-- 修复依据 db/migration/seed/rbac_permission_seed.sql 中的正确名称
UPDATE organ_permission SET permission_name = '定价列表' WHERE id = 467 AND permission_code = 'park:pricing:list';
UPDATE organ_permission SET permission_name = '定价详情' WHERE id = 468 AND permission_code = 'park:pricing:query';
UPDATE organ_permission SET permission_name = '新增定价' WHERE id = 469 AND permission_code = 'park:pricing:create';
UPDATE organ_permission SET permission_name = '修改定价' WHERE id = 470 AND permission_code = 'park:pricing:update';
UPDATE organ_permission SET permission_name = '删除定价' WHERE id = 471 AND permission_code = 'park:pricing:delete';
UPDATE organ_permission SET permission_name = '评分查看' WHERE id = 474 AND permission_code = 'park:score:query';
UPDATE organ_permission SET permission_name = '评分更新' WHERE id = 475 AND permission_code = 'park:score:update';
