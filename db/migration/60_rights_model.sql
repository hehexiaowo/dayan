-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 60_rights_model.sql  权益商品权益内容建模（对照《大雁养老养老权益文档》）
--
-- 核心变更（方案要点）：
-- 1. 人数两概念拆分：权益人数/构成 → goods_equity（holder_rule）；
--    单次服务使用人数/间数/晚数 → goods_service_item_rel（usage_rule）。
-- 2. 权益次数：quota_type 保留，年度配额统计锚点由自然年改为「激活周年」
--    （service_session.quota_reset_year 语义改为权益周年序号，由激活时间推算）。
-- 3. 配额归属：goods_equity.share_mode（0=按人独立，1=权益人共享池），
--    入库冻结到 equity_depot；service_session 落 use_person_id 支撑按人统计。
-- 4. 服务网络：rel 级 network_scope（NULL=业态全部机构，JSON=自选范围）。
-- 5. 入住权结构化：保证/优先/优惠 + discount_rate（90.00=门市价9折）。
-- 6. 终身语义：goods_equity.validity_type（1=固定天数，2=终身），
--    equity_activate.expire_time 放开 NULL。
-- 7. max_transferable 语义统一为「可转让次数」（0=不可，1/2/3=次数）。
-- 8. relation_with_holder 字典化（self/spouse/parent/parent_in_law/child/other）。
-- 9. 种子修正 GD00003~09 + 新增旅居随心住商品（GD00010 单次版 / GD00011 多次版）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. goods_equity：权益级配置扩展（人数构成 / 期限类型 / 配额归属）
-- ---------------------------------------------------------------------
ALTER TABLE `goods_equity`
  ADD COLUMN `validity_type` TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '权益期限类型（1=固定天数（valid_days生效），2=终身）' AFTER `person_count`,
  ADD COLUMN `holder_rule` TEXT NULL
    COMMENT '权益人构成规则JSON：{self:1,spouse:0|1,parent:0..4,designateAtActivation:bool}；person_count=1+spouse+parent' AFTER `validity_type`,
  ADD COLUMN `share_mode` TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '配额归属（0=按人独立配额，1=权益人共享池）' AFTER `holder_rule`,
  MODIFY COLUMN `max_transferable` TINYINT(3) NOT NULL DEFAULT 0
    COMMENT '可转让次数（0=不可转让，1/2/3=可转让N次）';

-- ---------------------------------------------------------------------
-- 2. goods_service_item_rel：权益内容扩展（网络范围 / 入住权 / 折扣 / 使用规则）
-- ---------------------------------------------------------------------
ALTER TABLE `goods_service_item_rel`
  ADD COLUMN `network_scope` TEXT NULL
    COMMENT '服务网络范围JSON：NULL=该服务所属业态全部在营机构；{"mode":"custom","parkCodes":[...]}=自选机构子集' AFTER `quota_type`,
  ADD COLUMN `admission_guaranteed` TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '保证入住权（0=无，1=有；长居/照护）' AFTER `network_scope`,
  ADD COLUMN `admission_priority` TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '优先入住权（0=无，1=有）' AFTER `admission_guaranteed`,
  ADD COLUMN `admission_discount` TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '优惠入住权/旅居优惠权（0=无，1=有）' AFTER `admission_priority`,
  ADD COLUMN `discount_rate` DECIMAL(4,2) NULL
    COMMENT '优惠折扣率（90.00=门市价9折；NULL=按协议未定，仅有优惠权标记）' AFTER `admission_discount`,
  ADD COLUMN `usage_rule` TEXT NULL
    COMMENT '单次使用规则JSON（随心住类）：{maxDaysPerUse,maxNightsPerUse,maxRoomsPerUse,maxGuestsPerUse,requireBeneficiaryCheckIn,advanceBookDays,depositAmount,refundPolicy:[{beforeHours,refundRate}],blackoutType,blackoutDays}' AFTER `discount_rate`;

-- ---------------------------------------------------------------------
-- 3. service_session：结构化权益人 + 年度配额锚点改激活周年
-- ---------------------------------------------------------------------
ALTER TABLE `service_session`
  ADD COLUMN `use_person_id` BIGINT NULL
    COMMENT '本次服务的权益人ID（equity_use_person.id；按人配额与审计用）' AFTER `item_code`,
  MODIFY COLUMN `quota_reset_year` INT NULL
    COMMENT '权益周年序号（quota_type=2 时记录消费发生在激活后第几个权益年，1起；由激活时间推算，跨周年自动重置）';

-- ---------------------------------------------------------------------
-- 4. equity_depot：入库快照扩展（期限/构成/配额归属，随卡冻结）
-- ---------------------------------------------------------------------
ALTER TABLE `equity_depot`
  ADD COLUMN `validity_type` TINYINT(1) NULL
    COMMENT '权益期限类型快照（1=固定天数,2=终身；入库时从goods_equity冻结）' AFTER `person_count`,
  ADD COLUMN `holder_rule` TEXT NULL
    COMMENT '权益人构成规则快照（入库时从goods_equity冻结，激活建人按此校验）' AFTER `validity_type`,
  ADD COLUMN `share_mode` TINYINT(1) NULL
    COMMENT '配额归属快照（0=按人独立,1=共享池；入库时从goods_equity冻结）' AFTER `holder_rule`;

-- ---------------------------------------------------------------------
-- 5. equity_activate.expire_time 放开 NULL（终身权益无过期时间）
-- ---------------------------------------------------------------------
ALTER TABLE `equity_activate`
  MODIFY COLUMN `expire_time` DATETIME NULL COMMENT '过期时间（终身权益为NULL）';

-- ---------------------------------------------------------------------
-- 6. relation_with_holder 字典 + 存量数据字典化
-- ---------------------------------------------------------------------
INSERT INTO `system_dict`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `parent_code`, `level`, `domain`,
   `sort_order`, `icon`, `css_class`, `extra`, `status`, `is_default`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('relation_with_holder', 'self',          '本人',             'self',          NULL, 1, 'equity', 0, NULL, NULL, NULL, 1, 1, '权益人与持有人关系-本人', NOW(), NOW(), 'system', 'system', 0),
  ('relation_with_holder', 'spouse',        '配偶',             'spouse',        NULL, 1, 'equity', 1, NULL, NULL, NULL, 1, 0, '权益人与持有人关系-配偶', NOW(), NOW(), 'system', 'system', 0),
  ('relation_with_holder', 'parent',        '父母（含公婆/岳父母）', 'parent',    NULL, 1, 'equity', 2, NULL, NULL, NULL, 1, 0, '权益人与持有人关系-父母', NOW(), NOW(), 'system', 'system', 0),
  ('relation_with_holder', 'parent_in_law', '公婆/岳父母',      'parent_in_law', NULL, 1, 'equity', 3, NULL, NULL, NULL, 1, 0, '权益人与持有人关系-公婆/岳父母', NOW(), NOW(), 'system', 'system', 0),
  ('relation_with_holder', 'child',         '子女',             'child',         NULL, 1, 'equity', 4, NULL, NULL, NULL, 1, 0, '权益人与持有人关系-子女', NOW(), NOW(), 'system', 'system', 0),
  ('relation_with_holder', 'other',         '其他',             'other',         NULL, 1, 'equity', 9, NULL, NULL, NULL, 1, 0, '权益人与持有人关系-其他', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `dict_name` = VALUES(`dict_name`), `updated_at` = NOW();

-- 存量自由文本 → 字典code
UPDATE `equity_use_person` SET `relation_with_holder` = 'self' WHERE `relation_with_holder` = '本人';
UPDATE `equity_use_person` SET `relation_with_holder` = 'spouse' WHERE `relation_with_holder` = '配偶';
UPDATE `equity_use_person` SET `relation_with_holder` = 'parent' WHERE `relation_with_holder` = '父母';
UPDATE `equity_use_person` SET `relation_with_holder` = 'parent_in_law' WHERE `relation_with_holder` IN ('公婆', '岳父母');
UPDATE `equity_use_person` SET `relation_with_holder` = 'child' WHERE `relation_with_holder` = '子女';
UPDATE `equity_use_person` SET `relation_with_holder` = 'other'
  WHERE `relation_with_holder` IS NULL OR `relation_with_holder` NOT IN
    ('self', 'spouse', 'parent', 'parent_in_law', 'child');

-- ---------------------------------------------------------------------
-- 7. 种子修正：终身权益 GD00003~08（终身 + 构成 + 共享池 + 转让次数）
--    对照文档：个人1人 / 本人+配偶 / 本人+配偶+指定父母(3/4/5人) / 本人+配偶+双方父母(6人)
-- ---------------------------------------------------------------------
UPDATE `goods_equity` SET `validity_type` = 2, `share_mode` = 1, `max_transferable` = 1,
  `holder_rule` = '{"self":1,"spouse":0,"parent":0,"designateAtActivation":false}'
  WHERE `goods_code` = 'GD00003';
UPDATE `goods_equity` SET `validity_type` = 2, `share_mode` = 1, `max_transferable` = 1,
  `holder_rule` = '{"self":1,"spouse":1,"parent":0,"designateAtActivation":false}'
  WHERE `goods_code` = 'GD00004';
UPDATE `goods_equity` SET `validity_type` = 2, `share_mode` = 1, `max_transferable` = 2,
  `holder_rule` = '{"self":1,"spouse":1,"parent":1,"designateAtActivation":true}'
  WHERE `goods_code` = 'GD00005';
UPDATE `goods_equity` SET `validity_type` = 2, `share_mode` = 1, `max_transferable` = 2,
  `holder_rule` = '{"self":1,"spouse":1,"parent":2,"designateAtActivation":true}'
  WHERE `goods_code` = 'GD00006';
UPDATE `goods_equity` SET `validity_type` = 2, `share_mode` = 1, `max_transferable` = 3,
  `holder_rule` = '{"self":1,"spouse":1,"parent":3,"designateAtActivation":true}'
  WHERE `goods_code` = 'GD00007';
UPDATE `goods_equity` SET `validity_type` = 2, `share_mode` = 1, `max_transferable` = 3,
  `holder_rule` = '{"self":1,"spouse":1,"parent":4,"designateAtActivation":false}'
  WHERE `goods_code` = 'GD00008';

-- GD00009 一年期养老管家：固定365天、本人（可服务持卡人自己的客户）、电子版不转让
UPDATE `goods_equity` SET `validity_type` = 1, `share_mode` = 1, `max_transferable` = 0,
  `holder_rule` = '{"self":1,"spouse":0,"parent":0,"designateAtActivation":false}'
  WHERE `goods_code` = 'GD00009';

-- ---------------------------------------------------------------------
-- 8. rel 入住权/折扣结构化（对照文档权益内容行）
--    终身权益：旅居=优惠权（折扣率未定）；长居/照护=保证+优先+优惠
--    一年期管家：旅居=优惠权（门市价9折）；长居/照护=优先+优惠（无保证）
-- ---------------------------------------------------------------------
UPDATE `goods_service_item_rel` SET `admission_discount` = 1
  WHERE `goods_code` IN ('GD00003','GD00004','GD00005','GD00006','GD00007','GD00008')
    AND `item_code` = 'SI00001';

UPDATE `goods_service_item_rel`
  SET `admission_guaranteed` = 1, `admission_priority` = 1, `admission_discount` = 1
  WHERE `goods_code` IN ('GD00003','GD00004','GD00005','GD00006','GD00007','GD00008')
    AND `item_code` IN ('SI00007','SI00005');

UPDATE `goods_service_item_rel` SET `admission_discount` = 1, `discount_rate` = 90.00
  WHERE `goods_code` = 'GD00009' AND `item_code` = 'SI00001';

UPDATE `goods_service_item_rel` SET `admission_priority` = 1, `admission_discount` = 1
  WHERE `goods_code` = 'GD00009' AND `item_code` IN ('SI00007','SI00005');

-- ---------------------------------------------------------------------
-- 9. 旅居随心住（电子卡）：新服务项目 + 商品（单次版/多次版）+ 配置 + 渠道
--    入住规则（文档）：3天2晚 / 1间 / 每间2人 / 提前15天 / 预定金500元 /
--    72h全退 48h退50% 24h不退 / 春节9天（除夕至初八）不可入住 / 权益人本人到场
-- ---------------------------------------------------------------------
INSERT INTO `service_item`
  (`item_code`, `item_name`, `item_category`, `item_subtype`, `item_value`,
   `cost_bearing`, `service_network`, `valid_days`, `max_use_count`,
   `description`, `sort_order`, `status`, `creator`, `updater`)
VALUES
  ('SI00008', '安排权益·旅居随心住入住', 1, 1, 0.00,
   1, '["*"]', 365, 1,
   '旅居随心住免费入住（每次最多3天2晚、1间房、每间2人；提前15天预订；预定金500元；春节9天不可入住；权益人本人到场办理）',
   0, 1, 'system', 'system')
ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO `goods_info`
  (`goods_code`, `goods_name`, `goods_short_name`, `goods_type`, `goods_description`, `summary`,
   `original_price`, `sale_price`, `price_unit`, `stock`, `goods_status`, `audit_status`,
   `creator`, `updater`, `created_at`, `updated_at`)
VALUES
  ('GD00010', '旅居随心住·单次版', '随心住单次', 1,
   '【旅居随心住·单次版（电子卡）】权益人：本人。\n核心权益：有效期内可在权益范围内的旅居养老机构任选一家免费入住1次（最多3天2晚、1间房、每间可住2人，需权益人本人到场）。\n入住规则：提前15天预订；春节9天（除夕至初八）不可入住；每次预订缴纳500元预定金，入住后1-3个工作日全额退还；入住前72小时取消全退、48小时取消退50%、24小时取消不退。',
   '电子卡：旅居机构免费入住1次（3天2晚/1间/2人）',
   100.00, 100.00, '元/份', -1, 1, 0,
   'system', 'system', NOW(), NOW()),
  ('GD00011', '旅居随心住·多次版', '随心住多次', 1,
   '【旅居随心住·多次版（电子卡）】权益人：本人。\n核心权益：有效期内可在权益范围内的旅居养老机构任选一家免费入住多次（每次最多3天2晚、1间房、每间可住2人，需权益人本人到场）。\n入住规则：提前15天预订；春节9天（除夕至初八）不可入住；每次预订缴纳500元预定金，入住后1-3个工作日全额退还；入住前72小时取消全退、48小时取消退50%、24小时取消不退。',
   '电子卡：旅居机构免费入住多次（3天2晚/1间/2人）',
   300.00, 300.00, '元/份', -1, 1, 0,
   'system', 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO `goods_equity`
  (`goods_code`, `person_count`, `validity_type`, `holder_rule`, `share_mode`,
   `valid_days`, `shelf_life_days`, `max_transferable`, `description`, `sort_order`, `status`)
VALUES
  ('GD00010', 1, 1, '{"self":1,"spouse":0,"parent":0,"designateAtActivation":false}', 1,
   365, 730, 0, '旅居随心住·单次版（本人，入住规则见关联服务项目）', 0, 1),
  ('GD00011', 1, 1, '{"self":1,"spouse":0,"parent":0,"designateAtActivation":false}', 1,
   365, 730, 0, '旅居随心住·多次版（本人，入住规则见关联服务项目）', 0, 1)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 随心住 rel：单次版=1次终身；多次版=3次终身（次数可按商务口径在权益配置中调整）
-- network_scope=NULL → 旅居业态全部在营机构；使用规则全量落在 usage_rule
INSERT INTO `goods_service_item_rel`
  (`goods_code`, `item_code`, `quantity`, `quota_type`,
   `network_scope`, `admission_guaranteed`, `admission_priority`, `admission_discount`,
   `discount_rate`, `usage_rule`, `sort_order`)
VALUES
  ('GD00010', 'SI00008', 1, 1,
   NULL, 0, 0, 0, NULL,
   '{"maxDaysPerUse":3,"maxNightsPerUse":2,"maxRoomsPerUse":1,"maxGuestsPerUse":2,"requireBeneficiaryCheckIn":true,"advanceBookDays":15,"depositAmount":500,"refundPolicy":[{"beforeHours":72,"refundRate":100},{"beforeHours":48,"refundRate":50},{"beforeHours":24,"refundRate":0}],"blackoutType":"spring_festival","blackoutDays":9}',
   0),
  ('GD00011', 'SI00008', 3, 1,
   NULL, 0, 0, 0, NULL,
   '{"maxDaysPerUse":3,"maxNightsPerUse":2,"maxRoomsPerUse":1,"maxGuestsPerUse":2,"requireBeneficiaryCheckIn":true,"advanceBookDays":15,"depositAmount":500,"refundPolicy":[{"beforeHours":72,"refundRate":100},{"beforeHours":48,"refundRate":50},{"beforeHours":24,"refundRate":0}],"blackoutType":"spring_festival","blackoutDays":9}',
   0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- channel_config_goods 无 (channel_code, goods_code) 唯一键，用 NOT EXISTS 防重
INSERT INTO `channel_config_goods`
  (`channel_code`, `goods_code`, `goods_type`, `custom_price`,
   `is_exclusive`, `status`, `sort_order`,
   `creator`, `updater`, `created_at`, `updated_at`)
SELECT 'CH00001', `g`.`goods_code`, 1, `g`.`sale_price`, 0, 1,
       CASE `g`.`goods_code` WHEN 'GD00010' THEN 17 ELSE 18 END,
       'system', 'system', NOW(), NOW()
FROM `goods_info` `g`
WHERE `g`.`goods_code` IN ('GD00010', 'GD00011')
  AND NOT EXISTS (
    SELECT 1 FROM `channel_config_goods` `c`
    WHERE `c`.`channel_code` = 'CH00001' AND `c`.`goods_code` = `g`.`goods_code` AND `c`.`deleted` = 0
  );
