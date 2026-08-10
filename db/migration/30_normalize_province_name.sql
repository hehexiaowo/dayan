SET NAMES utf8mb4;

-- 规范化直辖市 province 名称：统一加"市"后缀
-- 原数据中部分机构 province 写"北京"/"上海"/"天津"，导致省级聚合页面名称不一致

UPDATE park_info SET province = '北京市'
  WHERE province_code = '110000' AND province = '北京' AND deleted = 0;
UPDATE park_info SET province = '上海市'
  WHERE province_code = '310000' AND province = '上海' AND deleted = 0;
UPDATE park_info SET province = '天津市'
  WHERE province_code = '120000' AND province = '天津' AND deleted = 0;
UPDATE park_info SET province = '重庆市'
  WHERE province_code = '500000' AND province = '重庆' AND deleted = 0;
