-- 26_service_item_data_fix.sql
-- 数据修正：补全 service_item（活力长居）+ 重配 goods_service_item_rel（按产品真实权益）+ 清理孤儿
--
-- 产品权益对照（来自 goods_description）：
--   GD00003~GD00008 终身养老权益（6 档，N=person_count=1..6）：
--     旅居6次/年 + 活力长居N次/终身 + 照护N次/终身
--   GD00009 养老管家服务·一年期：
--     旅居10次/年 + 活力长居10次/年 + 照护10次/年

SET NAMES utf8mb4;

-- =====================================================================
-- 1. service_item 补全 + 规范化
-- =====================================================================

-- 1a. 补 item_value（SI00001 旅居已有1000，其余安排权益补合理面值）
UPDATE `service_item` SET `item_value` = 500.00
  WHERE `item_code` = 'SI00005' AND `item_value` IS NULL;  -- 照护长居

-- 1b. 新建活力长居（subtype=2，此前完全缺失）
INSERT INTO `service_item`
  (`item_code`, `item_name`, `item_category`, `item_subtype`, `item_value`,
   `cost_bearing`, `service_network`, `valid_days`, `max_use_count`,
   `description`, `sort_order`, `status`, `creator`, `updater`)
VALUES
  ('SI00007', '安排权益·活力长居', 1, 2, 500.00,
   0, '["*"]', 365, 10,
   '活力长者长期入住安排权益（自理型），含优先入住权、优惠入住权', 0, 1, 'system', 'system')
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 1c. 逻辑删除孤儿/重复项（未被任何产品 rel 引用）
--   SI00002/SI00003 重复"照护长居·全国通用"（被 SI00005 取代）
--   SI00004 孤儿"费用补贴"（产品描述无此权益）
UPDATE `service_item` SET `deleted` = 1, `deleted_at` = NOW()
  WHERE `item_code` IN ('SI00002', 'SI00003', 'SI00004') AND `deleted` = 0;

-- =====================================================================
-- 2. goods_service_item_rel 重配（先清旧 → 再插新）
--    旧 rel 只挂了 SI00005(照护)+SI00006(费用补贴)，与产品描述不符
-- =====================================================================

-- 2a. 物理删除所有旧 rel（GD00003~GD00009）
DELETE FROM `goods_service_item_rel`
  WHERE `goods_code` IN ('GD00003','GD00004','GD00005','GD00006','GD00007','GD00008','GD00009');

-- 2b. GD00003~GD00008 终身养老权益：旅居6/年 + 活力长居N/终身 + 照护N/终身
--     quota_type: 2=年度(旅居), 1=终身(长居/照护)
INSERT INTO `goods_service_item_rel`
  (`goods_code`, `item_code`, `quantity`, `quota_type`, `sort_order`)
VALUES
  -- GD00003 个人尊贵版 (1人): 长居1+照护1
  ('GD00003', 'SI00001', 6, 2, 0),   -- 旅居 6次/年
  ('GD00003', 'SI00007', 1, 1, 1),   -- 活力长居 1次/终身
  ('GD00003', 'SI00005', 1, 1, 2),   -- 照护长居 1次/终身
  -- GD00004 家庭尊享版 (2人): 长居2+照护2
  ('GD00004', 'SI00001', 6, 2, 0),
  ('GD00004', 'SI00007', 2, 1, 1),
  ('GD00004', 'SI00005', 2, 1, 2),
  -- GD00005 豪华3人版: 长居3+照护3
  ('GD00005', 'SI00001', 6, 2, 0),
  ('GD00005', 'SI00007', 3, 1, 1),
  ('GD00005', 'SI00005', 3, 1, 2),
  -- GD00006 豪华4人版: 长居4+照护4
  ('GD00006', 'SI00001', 6, 2, 0),
  ('GD00006', 'SI00007', 4, 1, 1),
  ('GD00006', 'SI00005', 4, 1, 2),
  -- GD00007 豪华5人版: 长居5+照护5
  ('GD00007', 'SI00001', 6, 2, 0),
  ('GD00007', 'SI00007', 5, 1, 1),
  ('GD00007', 'SI00005', 5, 1, 2),
  -- GD00008 至尊6人版: 长居6+照护6
  ('GD00008', 'SI00001', 6, 2, 0),
  ('GD00008', 'SI00007', 6, 1, 1),
  ('GD00008', 'SI00005', 6, 1, 2);

-- 2c. GD00009 养老管家服务·一年期：旅居10/年 + 长居10/年 + 照护10/年（全年度配额）
INSERT INTO `goods_service_item_rel`
  (`goods_code`, `item_code`, `quantity`, `quota_type`, `sort_order`)
VALUES
  ('GD00009', 'SI00001', 10, 2, 0),  -- 旅居 10次/年
  ('GD00009', 'SI00007', 10, 2, 1),  -- 活力长居 10次/年
  ('GD00009', 'SI00005', 10, 2, 2);  -- 照护长居 10次/年

-- 2d. SI00006（费用补贴）取消引用后逻辑删除（产品描述无补贴权益）
UPDATE `service_item` SET `deleted` = 1, `deleted_at` = NOW()
  WHERE `item_code` = 'SI00006' AND `deleted` = 0;

-- =====================================================================
-- 3. goods_equity.max_transferable 按产品描述分档
--    GD00003/04=1次, GD00005/06=2次, GD00007/08=3次
-- =====================================================================
UPDATE `goods_equity` SET `max_transferable` = 2
  WHERE `goods_code` IN ('GD00005', 'GD00006') AND `max_transferable` = 1;
UPDATE `goods_equity` SET `max_transferable` = 3
  WHERE `goods_code` IN ('GD00007', 'GD00008') AND `max_transferable` = 1;
