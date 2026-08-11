SET NAMES utf8mb4;

-- 网络展示配置：per-network 控制详情页头图和列表缩略图
-- JSON 结构：{"banners":["key1","key2"], "thumbnail":"key1"}
-- NULL = 未配置，fallback 到现有逻辑（头图=全部 type=1 图，缩略图=不显示）

ALTER TABLE park_info
  ADD COLUMN vital_config   TEXT DEFAULT NULL COMMENT '活力长居展示配置JSON（banners+thumbnail）' AFTER network_tags,
  ADD COLUMN care_config    TEXT DEFAULT NULL COMMENT '照护长居展示配置JSON（banners+thumbnail）' AFTER vital_config,
  ADD COLUMN sojourn_config TEXT DEFAULT NULL COMMENT '旅居展示配置JSON（banners+thumbnail）' AFTER care_config;
