-- =====================================================================
-- sync_assets.sql  将子表图片同步注册到 park_asset 素材库
--
-- 设计意图（park_asset_merge.sql 注释）：
--   park_asset 是机构所有素材的统一管理视图。
--   source_type 追踪来源（room_type/food_type/display_block/...）。
--   之前迁移只在 park_asset 注册了 head_images（source_type='media_mgmt'），
--   但 room_type / food_type / display_block 子表的图片没有注册，
--   导致素材库数量 < 各处实际引用的图片总数。
--
-- 本脚本：从子表提取所有图片 key → 去重后 INSERT IGNORE 到 park_asset
--   （使用 LEFT JOIN 排除已存在的，避免重复注册）
-- =====================================================================

-- 工作目录 temp table，收集所有需要注册的图片
DROP TEMPORARY TABLE IF EXISTS tmp_images_to_register;
CREATE TEMPORARY TABLE tmp_images_to_register (
  park_code   VARCHAR(64) NOT NULL,
  img_key     VARCHAR(500) NOT NULL,
  source_type VARCHAR(30) NOT NULL,
  ref_code    VARCHAR(64),
  sort_order  INT DEFAULT 0,
  UNIQUE KEY uk_img (park_code, img_key)
);

-- 1. room_type: cover_image + images JSON array
INSERT IGNORE INTO tmp_images_to_register (park_code, img_key, source_type, ref_code, sort_order)
SELECT park_code, cover_image, 'room_type', room_type_code, 0
FROM park_room_type
WHERE park_code BETWEEN 'PK00003' AND 'PK00022'
  AND cover_image IS NOT NULL AND deleted = 0;

-- room_type.images JSON array → 逐个展开
INSERT IGNORE INTO tmp_images_to_register (park_code, img_key, source_type, ref_code, sort_order)
SELECT rt.park_code, jt.img, 'room_type', rt.room_type_code, jt.idx
FROM park_room_type rt,
JSON_TABLE(rt.images, '$[*]' COLUMNS (
  idx FOR ORDINALITY,
  img VARCHAR(500) PATH '$'
)) jt
WHERE rt.park_code BETWEEN 'PK00003' AND 'PK00022'
  AND rt.images IS NOT NULL
  AND JSON_VALID(rt.images)
  AND rt.deleted = 0;

-- 2. food_type: cover_image
INSERT IGNORE INTO tmp_images_to_register (park_code, img_key, source_type, ref_code, sort_order)
SELECT park_code, cover_image, 'food_type', food_type_code, 0
FROM park_food_type
WHERE park_code BETWEEN 'PK00003' AND 'PK00022'
  AND cover_image IS NOT NULL AND deleted = 0;

-- (food_type has no images JSON array, only cover_image handled above)

-- 3. display_block: images JSON array
INSERT IGNORE INTO tmp_images_to_register (park_code, img_key, source_type, ref_code, sort_order)
SELECT db.park_code, jt.img, 'display_block', db.block_type, jt.idx
FROM park_display_block db,
JSON_TABLE(db.images, '$[*]' COLUMNS (
  idx FOR ORDINALITY,
  img VARCHAR(500) PATH '$'
)) jt
WHERE db.park_code BETWEEN 'PK00003' AND 'PK00022'
  AND db.images IS NOT NULL
  AND JSON_VALID(db.images)
  AND db.deleted = 0;

-- 4. park_info brand_logo (if set)
INSERT IGNORE INTO tmp_images_to_register (park_code, img_key, source_type, ref_code, sort_order)
SELECT park_code, brand_logo, 'park_info', park_code, 0
FROM park_info
WHERE park_code BETWEEN 'PK00003' AND 'PK00022'
  AND brand_logo IS NOT NULL AND brand_logo != '';

-- Show what we collected
SELECT '=== Images to register (excluding duplicates) ===' AS '';
SELECT COUNT(*) AS total_to_register FROM tmp_images_to_register;

SELECT source_type, COUNT(*) AS cnt
FROM tmp_images_to_register
GROUP BY source_type ORDER BY source_type;

-- 5. Filter out images that already exist in park_asset (same park_code + asset_url)
DELETE t FROM tmp_images_to_register t
INNER JOIN park_asset a
  ON a.park_code = t.park_code
  AND a.asset_url = t.img_key
  AND a.deleted = 0;

SELECT '=== After removing existing ===' AS '';
SELECT COUNT(*) AS new_to_register FROM tmp_images_to_register;

-- 6. Insert the new images into park_asset
INSERT INTO park_asset (
  park_code, asset_type, asset_url, asset_name,
  source_type, source_ref_code, sort_order, status,
  created_at, updated_at, creator, deleted
)
SELECT
  park_code, 1, img_key,
  SUBSTRING_INDEX(SUBSTRING_INDEX(img_key, '/', -1), '.', 1),
  source_type, ref_code, sort_order, 1,
  NOW(), NOW(), 'migration', 0
FROM tmp_images_to_register;

SELECT CONCAT('=== Inserted ', ROW_COUNT(), ' new assets ===') AS result;

-- 7. Final verification
DROP TEMPORARY TABLE IF EXISTS tmp_images_to_register;
