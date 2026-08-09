-- =====================================================================
-- 05_park.sql  养老机构域（park_）
-- 域说明：养老机构核心资产——主信息、素材库、设施/服务/房型/照护/餐饮类型与费用、顾问、周边
-- 表数：13（park_info + park_asset + facility_type + service_type + adviser + periphery + room_type + care_type + food_type + pricing + pricing_item + score + display_block）
-- 注：原 4 张 media 表（media_image/video/file/vr）已合并为 park_asset，见 park_asset_merge.sql
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.5
-- 主键策略：全部为平台共享表（AUTO_INCREMENT）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.5.1 park_info 养老机构核心信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_info`;
CREATE TABLE `park_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构唯一编码',
  `full_name` VARCHAR(128) DEFAULT NULL COMMENT '机构名称',
  `short_name` VARCHAR(32) DEFAULT NULL COMMENT '机构简称',
  `supplier_code` VARCHAR(50) NOT NULL COMMENT '所属供应商编码',
  `brand` VARCHAR(64) DEFAULT NULL COMMENT '品牌名称',
  `brand_introduction` TEXT DEFAULT NULL COMMENT '品牌简介',
  `brand_logo` VARCHAR(500) DEFAULT NULL COMMENT '品牌Logo',
  `operation_subject` VARCHAR(128) DEFAULT NULL COMMENT '运营主体',
  `operation_subject_description` TEXT DEFAULT NULL COMMENT '运营主体介绍',
  `important_shareholders` TEXT DEFAULT NULL COMMENT '重要股东',
  `partner_company` VARCHAR(255) DEFAULT NULL COMMENT '合作公司主体',
  `business_license_no` VARCHAR(64) DEFAULT NULL COMMENT '营业执照号',
  `business_bd` VARCHAR(32) DEFAULT NULL COMMENT '商务BD',
  `ability_type` TINYINT(4) DEFAULT NULL COMMENT '机构类型（1=持续照料社区CCRC, 2=综合性养老院, 3=养老公寓CB, 4=认知症照护机构, 5=社区嵌入式照料中心CC, 6=居家照护HC, 7=护理院NH, 8=活力长者社区AC, 9=敬老院）',
  `ability_type_description` VARCHAR(32) DEFAULT NULL COMMENT '机构类型描述',
  `nature_type` TINYINT(4) DEFAULT NULL COMMENT '机构性质（1=公办, 2=民办, 3=险资背景, 4=集团旗下, 5=房产旗下, 6=公办民营）',
  `nature_type_description` VARCHAR(32) DEFAULT NULL COMMENT '机构性质描述',
  `specialty_tag` VARCHAR(256) DEFAULT NULL COMMENT '特色标签（多个逗号隔开）',
  `dayan_level` TINYINT(2) DEFAULT NULL COMMENT '评级（1=S, 2=A, 3=B, 4=C, 5=D, 6=E）',
  `province` VARCHAR(16) DEFAULT NULL COMMENT '省',
  `province_code` VARCHAR(20) DEFAULT NULL COMMENT '省编码',
  `city` VARCHAR(16) DEFAULT NULL COMMENT '城市',
  `city_code` VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
  `district` VARCHAR(16) DEFAULT NULL COMMENT '区',
  `district_code` VARCHAR(20) DEFAULT NULL COMMENT '区编码',
  `address` VARCHAR(256) DEFAULT NULL COMMENT '具体地址',
  `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `service_hotline` VARCHAR(32) DEFAULT NULL COMMENT '客服电话',
  `base_description` TEXT DEFAULT NULL COMMENT '机构介绍',
  `specialty_description` VARCHAR(512) DEFAULT NULL COMMENT '机构特色',
  `total_area` VARCHAR(32) DEFAULT NULL COMMENT '占地面积',
  `building_area` VARCHAR(32) DEFAULT NULL COMMENT '建筑面积',
  `green_area_rate` VARCHAR(32) DEFAULT NULL COMMENT '绿化率',
  `total_beds` INT(11) DEFAULT NULL COMMENT '总床位数',
  `available_beds` INT(11) DEFAULT NULL COMMENT '可用床位数',
  `occupancy_rate` VARCHAR(32) DEFAULT NULL COMMENT '已入住率',
  `staff_count` INT(11) DEFAULT NULL COMMENT '员工总数',
  `nurse_count` INT(11) DEFAULT NULL COMMENT '护理人员数',
  `nurse_patient_ratio` VARCHAR(20) DEFAULT NULL COMMENT '护患比（如1:5）',
  `min_price_display` INT(11) DEFAULT NULL COMMENT '最低月费（元）',
  `max_price_display` INT(11) DEFAULT NULL COMMENT '最高月费（元）',
  `price_unit` VARCHAR(32) DEFAULT NULL COMMENT '价格单位',
  `check_in_age_min` INT(11) DEFAULT NULL COMMENT '入住最低年龄',
  `check_in_age_max` INT(11) DEFAULT NULL COMMENT '入住最高年龄',
  `check_in_description` VARCHAR(512) DEFAULT NULL COMMENT '入住说明',
  `deposit_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '押金金额',
  `deposit_description` VARCHAR(500) DEFAULT NULL COMMENT '押金说明',
  `contract_period` TINYINT(2) DEFAULT NULL COMMENT '合同期限（1=月签, 2=季签, 3=半年, 4=年签）',
  `sort_order` INT(11) DEFAULT 0 COMMENT '排序号',
  `is_hot` TINYINT(2) DEFAULT NULL COMMENT '平台内评级（1=付费广告, 2=热门）',
  `sub_script` VARCHAR(11) DEFAULT NULL COMMENT '首页角标（1=最新, 2=最热, 3=优惠, 4=店庆）',
  `operate_status` TINYINT(1) DEFAULT NULL COMMENT '运营状态（0=待审核, 1=已上线, 2=已下架, 3=暂停营业；PARK_SM 驱动）',
  `opening_time` DATETIME DEFAULT NULL COMMENT '开业时间',
  `online_time` DATETIME DEFAULT NULL COMMENT '上架时间',
  `offline_time` DATETIME DEFAULT NULL COMMENT '下架时间',
  `add_platform_time` DATETIME DEFAULT NULL COMMENT '加入平台时间',
  `is_published` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否已发布（0=未发布, 1=已发布）',
  `view_count` INT(11) NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `collect_count` INT(11) NOT NULL DEFAULT 0 COMMENT '收藏次数',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `version` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '数据版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_park_code` (`park_code`),
  KEY `idx_full_name` (`full_name`(50)),
  KEY `idx_province_code` (`province_code`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_district_code` (`district_code`),
  KEY `idx_ability_type` (`ability_type`),
  KEY `idx_nature_type` (`nature_type`),
  KEY `idx_dayan_level` (`dayan_level`),
  KEY `idx_operate_status` (`operate_status`),
  KEY `idx_brand` (`brand`(20)),
  KEY `idx_min_price_display` (`min_price_display`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_is_published` (`is_published`),
  FULLTEXT KEY `ft_full_name` (`full_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养老机构核心信息';

-- ---------------------------------------------------------------------
-- 3.5.2 park_media_image 机构图片资源
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_media_image`;
CREATE TABLE `park_media_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `image_name` VARCHAR(200) DEFAULT NULL COMMENT '图片名称',
  `image_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '图片类型（1=外观, 2=大堂, 3=房间, 4=餐厅, 5=活动区, 6=花园, 7=医疗区, 8=户型, 9=文娱生活, 10=康养状况, 11=其他）',
  `image_description` VARCHAR(500) DEFAULT NULL COMMENT '图片描述',
  `width` INT(11) DEFAULT NULL COMMENT '图片宽度（像素）',
  `height` INT(11) DEFAULT NULL COMMENT '图片高度（像素）',
  `file_size` INT(11) DEFAULT NULL COMMENT '文件大小（KB）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_cover` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否封面（0=否, 1=是）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏, 1=显示）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_image_type` (`image_type`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构图片资源';

-- ---------------------------------------------------------------------
-- 3.5.3 park_media_video 机构视频资源
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_media_video`;
CREATE TABLE `park_media_video` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `video_url` VARCHAR(500) NOT NULL COMMENT '视频URL',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `video_name` VARCHAR(200) DEFAULT NULL COMMENT '视频名称',
  `video_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '视频类型（1=宣传视频, 2=环境展示, 3=活动记录）',
  `video_description` VARCHAR(500) DEFAULT NULL COMMENT '视频描述',
  `duration` INT(11) DEFAULT NULL COMMENT '时长（秒）',
  `file_size` INT(11) DEFAULT NULL COMMENT '文件大小（KB）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏, 1=显示）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_video_type` (`video_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构视频资源';

-- ---------------------------------------------------------------------
-- 3.5.4 park_media_file 机构文件资源
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_media_file`;
CREATE TABLE `park_media_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件URL',
  `file_name` VARCHAR(200) NOT NULL COMMENT '文件名称',
  `file_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '文件类型（1=资质文件, 2=合同文件, 3=宣传资料, 4=费用文档, 5=其他）',
  `file_format` VARCHAR(20) NOT NULL COMMENT '文件格式（pdf/doc/xls等）',
  `file_size` INT(11) DEFAULT NULL COMMENT '文件大小（KB）',
  `file_description` VARCHAR(500) DEFAULT NULL COMMENT '文件描述',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏, 1=显示）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_file_type` (`file_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构文件资源';

-- ---------------------------------------------------------------------
-- 3.5.5 park_media_vr 机构VR资源
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_media_vr`;
CREATE TABLE `park_media_vr` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `vr_url` VARCHAR(500) NOT NULL COMMENT 'VR全景链接',
  `vr_provider` VARCHAR(100) DEFAULT NULL COMMENT 'VR服务提供商',
  `vr_name` VARCHAR(200) DEFAULT NULL COMMENT 'VR资源名称',
  `vr_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT 'VR类型（1=全景VR, 2=3D模型, 3=视频VR）',
  `thumbnail_url` VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
  `vr_description` VARCHAR(500) DEFAULT NULL COMMENT 'VR描述',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏, 1=显示）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_code` (`park_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构VR资源';

-- ---------------------------------------------------------------------
-- 3.5.6 park_facility_type 机构配套设施类型
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_facility_type`;
CREATE TABLE `park_facility_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `facility_type_code` VARCHAR(50) NOT NULL COMMENT '设施类型编码',
  `facility_type_name` VARCHAR(100) NOT NULL COMMENT '设施类型名称（如"健身房"、"棋牌室"、"医疗室"、"阅览室"）',
  `facility_type_category` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '设施类别（1=休闲娱乐, 2=医疗健康, 3=运动健身, 4=文化教育, 5=生活服务, 6=安全保障）',
  `building_name` VARCHAR(100) DEFAULT NULL COMMENT '所在楼栋（如"A栋"、"南楼"）',
  `floor` VARCHAR(20) DEFAULT NULL COMMENT '所在楼层',
  `area` DECIMAL(8,2) DEFAULT NULL COMMENT '面积（平方米）',
  `capacity` INT(11) DEFAULT NULL COMMENT '最大容纳人数',
  `open_time` VARCHAR(100) DEFAULT NULL COMMENT '开放时间（如"08:00-20:00"）',
  `facility_type_description` TEXT DEFAULT NULL COMMENT '设施详细描述',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `images` TEXT DEFAULT NULL COMMENT '设施图片URL列表（JSON数组）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_facility_type_code` (`park_code`, `facility_type_code`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_facility_type_category` (`facility_type_category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构配套设施类型';

-- ---------------------------------------------------------------------
-- 3.5.7 park_service_type 机构服务类型
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_service_type`;
CREATE TABLE `park_service_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `service_type_code` VARCHAR(50) NOT NULL COMMENT '服务类型编码',
  `service_type_name` VARCHAR(100) NOT NULL COMMENT '服务类型名称（如"24小时护理"、"康复训练"、"心理疏导"）',
  `service_type_category` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '服务类别（1=生活照料, 2=医疗健康, 3=康复训练, 4=文化娱乐, 5=心理关怀, 6=其他）',
  `service_type_description` TEXT DEFAULT NULL COMMENT '服务详细描述',
  `service_type_frequency` VARCHAR(100) DEFAULT NULL COMMENT '服务频次（如"每日3次"、"按需"）',
  `service_type_duration` VARCHAR(50) DEFAULT NULL COMMENT '服务时长（如"每次1小时"）',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '服务图片URL',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_type_code` (`park_code`, `service_type_code`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_service_type_category` (`service_type_category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构服务类型';

-- ---------------------------------------------------------------------
-- 3.5.8 park_adviser 机构顾问信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_adviser`;
CREATE TABLE `park_adviser` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `adviser_name` VARCHAR(100) NOT NULL COMMENT '顾问姓名',
  `adviser_title` VARCHAR(100) DEFAULT NULL COMMENT '顾问头衔（如"资深顾问"、"护理主管"）',
  `adviser_image` VARCHAR(500) DEFAULT NULL COMMENT '顾问照片URL',
  `adviser_content` TEXT DEFAULT NULL COMMENT '顾问介绍/专业背景',
  `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '顾问联系电话',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否首席顾问（0=否, 1=是）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏, 1=显示）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_is_primary` (`is_primary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构顾问信息';

-- ---------------------------------------------------------------------
-- 3.5.9 park_periphery 机构周边信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_periphery`;
CREATE TABLE `park_periphery` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `periphery_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '周边类型（1=交通-公交, 2=交通-地铁, 3=交通-自驾, 4=景点, 5=医疗, 6=购物, 7=公园, 8=其他）',
  `place_name` VARCHAR(200) NOT NULL COMMENT '地点名称',
  `place_address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址/路线描述',
  `distance` VARCHAR(50) DEFAULT NULL COMMENT '距离描述（如"500米"、"步行10分钟"）',
  `detail_description` TEXT DEFAULT NULL COMMENT '详细描述',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏, 1=显示）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_periphery_type` (`periphery_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构周边信息';

-- ---------------------------------------------------------------------
-- 3.5.10 park_room_type 养老机构房间类型
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_room_type`;
CREATE TABLE `park_room_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `room_type_code` VARCHAR(50) NOT NULL COMMENT '房间类型编码',
  `room_type_name` VARCHAR(100) NOT NULL COMMENT '房间类型名称（如"豪华南向单人间"）',
  `stay_type` TINYINT(2) NOT NULL COMMENT '居住类型（1=长居, 2=旅居）',
  `building_name` VARCHAR(100) DEFAULT NULL COMMENT '楼栋名称（如"A栋"、"南楼"）',
  `floor` VARCHAR(20) DEFAULT NULL COMMENT '所在楼层',
  `room_category` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '房间类别（1=单人间, 2=双人间, 3=多人间, 4=套间, 5=VIP房）',
  `area` DECIMAL(8,2) DEFAULT NULL COMMENT '房间面积（平方米）',
  `orientation` VARCHAR(20) DEFAULT NULL COMMENT '朝向（如"南", "东南", "西南"）',
  `bed_count` INT(11) NOT NULL DEFAULT 1 COMMENT '床位数',
  `total_rooms` INT(11) NOT NULL DEFAULT 0 COMMENT '该类型房间总数',
  `available_rooms` INT(11) NOT NULL DEFAULT 0 COMMENT '可入住数',
  `has_bathroom` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '独立卫生间（0=否, 1=是）',
  `has_kitchen` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '独立厨房（0=否, 1=是）',
  `has_balcony` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '有阳台（0=否, 1=是）',
  `has_tv` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '有电视（0=否, 1=是）',
  `has_aircon` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '有空调（0=否, 1=是）',
  `has_fridge` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '有冰箱（0=否, 1=是）',
  `has_washer` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '有洗衣机（0=否, 1=是）',
  `has_wifi` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '有WiFi（0=否, 1=是）',
  `has_emergency` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '有紧急呼叫（0=否, 1=是）',
  `has_monitor` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '有监控（0=否, 1=是）',
  `facilities` TEXT DEFAULT NULL COMMENT '配套设施详情（JSON数组）',
  `description` TEXT DEFAULT NULL COMMENT '房间详细说明',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `images` TEXT DEFAULT NULL COMMENT '房间图片URL列表（JSON数组）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停售, 1=在售）',
  `design_description` TEXT DEFAULT NULL COMMENT '户型设计描述',
  `design_image` VARCHAR(500) DEFAULT NULL COMMENT '户型图URL',
  `additional_images` TEXT DEFAULT NULL COMMENT '其他户型图片（JSON数组）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_type_code` (`park_code`, `room_type_code`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_stay_type` (`stay_type`),
  KEY `idx_room_category` (`room_category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养老机构房间类型';

-- ---------------------------------------------------------------------
-- 3.5.11 park_pricing 机构统一定价方案
-- ---------------------------------------------------------------------
-- 合并原 park_room_price / park_care_price / park_food_price / park_facility_price / park_service_price 五张表。
-- 养老机构定价四要素：押金(charge_type=4)、房间费(1)、照护等级费(2)、餐费(3)，外加设施费(5)/服务费(6)。
-- charge_type 统一费类；ref_type+ref_code 关联具体 type 表（room_type/care_type/food_type/facility_type/service_type）。
-- billing_cycle（枚举：月/季/半年/年/一次性）兼容 room/care/food；price_unit（自由文本）兼容 facility/service。
-- current_key 生成列 + uk_current 唯一索引 = DB 级保证 is_current=1 在同维度下唯一。
-- version 字段 + @Version 注解 = MyBatis-Plus 乐观锁。
DROP TABLE IF EXISTS `park_pricing`;
CREATE TABLE `park_pricing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) NOT NULL COMMENT '机构编码',
  `plan_name` VARCHAR(100) DEFAULT NULL COMMENT '方案名称（如"豪华单人间·月费"）',
  `charge_type` TINYINT NOT NULL COMMENT '费类（1=房间费 2=照护费 3=餐费 4=押金 5=设施费 6=服务费 9=其他）',
  `ref_type` VARCHAR(20) NOT NULL COMMENT '关联类型（room_type/care_type/food_type/facility_type/service_type/park）',
  `ref_code` VARCHAR(64) NOT NULL COMMENT '关联编码',
  `ref_name` VARCHAR(100) DEFAULT NULL COMMENT '关联名称（冗余）',
  `billing_cycle` TINYINT DEFAULT NULL COMMENT '计费周期（1=月 2=季 3=半年 4=年 5=一次性）',
  `price_unit` VARCHAR(50) DEFAULT NULL COMMENT '自由文本计费单位（设施/服务的 次/小时/场）',
  `original_price` DECIMAL(12,2) DEFAULT NULL COMMENT '原价',
  `sale_price` DECIMAL(12,2) NOT NULL COMMENT '售价',
  `discount_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '折扣率',
  `price_description` VARCHAR(500) DEFAULT NULL COMMENT '价格说明',
  `includes_items` TEXT DEFAULT NULL COMMENT '包含项目（JSON数组）',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  `expire_date` DATE DEFAULT NULL COMMENT '失效日期（NULL=长期有效）',
  `is_current` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否当前生效价格（0=历史 1=当前）',
  `is_promotion` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否促销价',
  `promotion_description` VARCHAR(200) DEFAULT NULL COMMENT '促销说明',
  `price_change_reason` VARCHAR(500) DEFAULT NULL COMMENT '价格变更原因',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停用 1=启用）',
  `version` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  `current_key` VARCHAR(200) GENERATED ALWAYS AS
    (CASE WHEN `is_current` = 1 AND `deleted` = 0
     THEN CONCAT(`park_code`, '|', `charge_type`, '|', `ref_code`, '|', COALESCE(CAST(`billing_cycle` AS CHAR), '0'))
     ELSE NULL END) STORED COMMENT '当前价唯一键（生成列）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_current` (`current_key`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_charge_type` (`charge_type`),
  KEY `idx_ref` (`ref_type`, `ref_code`),
  KEY `idx_is_current` (`is_current`),
  KEY `idx_effective_date` (`effective_date`),
  KEY `idx_park_charge` (`park_code`, `charge_type`, `billing_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构统一定价方案';

-- ---------------------------------------------------------------------
-- 3.5.11b park_pricing_item 机构定价明细行（套餐关联）
-- ---------------------------------------------------------------------
-- 每条 pricing 创建时自动创建一条 pricing_item（主行，item_type/ref_code 与 pricing 冗余一致）。
-- 套餐场景：一条 pricing 关联多条 pricing_item（如"全包月套餐"→ room_type + care_type + food_type）。
DROP TABLE IF EXISTS `park_pricing_item`;
CREATE TABLE `park_pricing_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pricing_id` BIGINT NOT NULL COMMENT 'FK→park_pricing.id',
  `park_code` VARCHAR(64) NOT NULL COMMENT '机构编码',
  `item_type` VARCHAR(20) NOT NULL COMMENT '关联类型（room_type/care_type/food_type/facility_type/service_type）',
  `item_code` VARCHAR(64) NOT NULL COMMENT '关联编码',
  `item_name` VARCHAR(100) DEFAULT NULL COMMENT '关联名称（冗余）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_pricing_id` (`pricing_id`),
  KEY `idx_item` (`park_code`, `item_type`, `item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构定价明细行（套餐关联）';

-- ---------------------------------------------------------------------
-- 3.5.11c park_score 机构评分（独立表，避免写热点）
-- ---------------------------------------------------------------------
-- 从 park_info 拆出：评分字段高频写（用户评价），与机构主信息低频编辑分离。
DROP TABLE IF EXISTS `park_score`;
CREATE TABLE `park_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) NOT NULL COMMENT '机构编码',
  `score_total` INT(11) DEFAULT NULL COMMENT '总评分',
  `score_environment` INT(11) DEFAULT NULL COMMENT '环境评分',
  `score_recreation` INT(11) DEFAULT NULL COMMENT '文娱评分',
  `score_nursing` INT(11) DEFAULT NULL COMMENT '医养护理评分',
  `score_food` INT(11) DEFAULT NULL COMMENT '餐食精细评分',
  `score_service` INT(11) DEFAULT NULL COMMENT '服务品质评分',
  `score_price` INT(11) DEFAULT NULL COMMENT '价格评分',
  `score_description` VARCHAR(255) DEFAULT NULL COMMENT '评分描述',
  `version` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_park_code` (`park_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构评分（独立表）';

-- ---------------------------------------------------------------------
-- 3.5.12 park_care_type 养老机构照护类型
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_care_type`;
CREATE TABLE `park_care_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `care_type_code` VARCHAR(50) NOT NULL COMMENT '照护类型编码',
  `care_type_name` VARCHAR(100) NOT NULL COMMENT '照护类型名称（如"自理型"、"半失能"、"失能"、"失智"）',
  `care_level` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '照护等级（1=特级护理, 2=一级护理, 3=二级护理, 4=三级护理, 5=生活自理）',
  `care_target` VARCHAR(500) DEFAULT NULL COMMENT '适用人群描述',
  `care_items` TEXT DEFAULT NULL COMMENT '护理项目明细（JSON数组）',
  `care_frequency` VARCHAR(100) DEFAULT NULL COMMENT '护理频次（如"每日3次"、"24小时"）',
  `nurse_patient_ratio` VARCHAR(20) DEFAULT NULL COMMENT '护患比（如"1:3"）',
  `assessment_criteria` TEXT DEFAULT NULL COMMENT '评估标准说明',
  `description` TEXT DEFAULT NULL COMMENT '照护类型详细描述',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停售, 1=在售）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_care_type_code` (`park_code`, `care_type_code`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_care_level` (`care_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养老机构照护类型';

-- ---------------------------------------------------------------------
-- 3.5.13 (deleted) park_care_price → 已合并入 park_pricing（charge_type=2）
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 3.5.14 park_food_type 养老机构餐饮类型
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_food_type`;
CREATE TABLE `park_food_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) DEFAULT NULL COMMENT '机构编码',
  `food_type_code` VARCHAR(50) NOT NULL COMMENT '餐饮类型编码',
  `food_type_name` VARCHAR(100) NOT NULL COMMENT '餐饮类型名称（如"普通餐"、"营养餐"、"糖尿病餐"）',
  `meal_plan` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '餐饮方案（1=三餐, 2=三餐两点, 3=三餐三点, 4=自选）',
  `diet_features` VARCHAR(500) DEFAULT NULL COMMENT '饮食特色（JSON数组）',
  `sample_menu` TEXT DEFAULT NULL COMMENT '示例菜单（JSON格式）',
  `special_diet` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否支持特殊饮食（0=否, 1=是）',
  `special_diet_description` VARCHAR(500) DEFAULT NULL COMMENT '特殊饮食说明',
  `description` TEXT DEFAULT NULL COMMENT '详细描述',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停售, 1=在售）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_type_code` (`park_code`, `food_type_code`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_meal_plan` (`meal_plan`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养老机构餐饮类型';

-- ---------------------------------------------------------------------
-- 3.5.15 (deleted) park_food_price → 已合并入 park_pricing（charge_type=3）
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 3.5.16 (deleted) park_facility_price → 已合并入 park_pricing（charge_type=5）
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 3.5.17 (deleted) park_service_price → 已合并入 park_pricing（charge_type=6）
-- ---------------------------------------------------------------------

-- 3.5.18 park_display_block 机构展示内容板块（C端详情页）
-- 替代远程 wkb_yl 主表/ext 表散落的 40+ 展示列（entertainment_life_*/health_status_*/live_env_*/catering_* 等）。
-- 一个机构 = N 个板块，每个板块 = 类型 + 标题 + 富文本正文 + 图片列表(JSON) + 图片描述(JSON)。
-- block_type 词库：brand_intro/payment_way/live_env/catering/entertainment/health_status/checkin_guide/fee_explain/custom，
-- 新增板块类型只加数据，不需要改表结构。
DROP TABLE IF EXISTS `park_display_block`;
CREATE TABLE `park_display_block` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) NOT NULL COMMENT '机构编码',
  `block_type` VARCHAR(50) NOT NULL COMMENT '板块类型（brand_intro/payment_way/live_env/catering/entertainment/health_status/checkin_guide/fee_explain/custom）',
  `block_title` VARCHAR(100) DEFAULT NULL COMMENT '板块标题（C端展示用，如"居住环境"）',
  `content` TEXT DEFAULT NULL COMMENT '富文本内容（HTML）',
  `images` TEXT DEFAULT NULL COMMENT '图片key列表（JSON数组，如["park/a.jpg","park/b.jpg"]）',
  `image_descriptions` TEXT DEFAULT NULL COMMENT '图片描述列表（JSON数组，与images一一对应）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏, 1=显示）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_park_type` (`park_code`, `block_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构展示内容板块（C端详情页）';
