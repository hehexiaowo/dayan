SET NAMES utf8mb4;
-- =====================================================================
-- 54_dict_unify.sql  字典体系统一：双表收敛为单表 system_dict，单菜单管理
--
-- 背景：system_dict_common（通用）与 system_dict_business（业务）双表双页，
--       概念重叠管理割裂。统一收敛为一张 system_dict + 字典管理单菜单。
-- 1. common 扩列 domain（业务域标注）+ extra（JSON 扩展，承接内容分类
--    coverImage/isVisible 等），完全覆盖 business 表能力；
-- 2. business 全部启用行并入（level=1、is_default=0、css_class 置空，
--    icon/extra/domain/sort/status/remark 原样保留）；
-- 3. 清理：业务字典菜单（此前系直插 DB，无种子）+ system:dict-biz:* 权限
--    与角色授权 + DROP 业务字典表；
-- 4. system_dict_common 改名 system_dict（单字典后无需 common 区隔）。
-- 链路：41/45/47/50/53 仍按原样向 business 表写入（fresh init 阶段），
--       本文件随后统一搬迁收口，两条路径（全新初始化/存量库）收敛一致，
--       种子文件零改动。
-- =====================================================================

ALTER TABLE `system_dict_common`
  ADD COLUMN `domain` VARCHAR(32) DEFAULT NULL COMMENT '业务域（通用字典为空；业务语义字典标注所属域，如 park/content）' AFTER `level`,
  ADD COLUMN `extra` JSON DEFAULT NULL COMMENT '扩展属性（如内容分类的 coverImage/isVisible）' AFTER `css_class`,
  ADD KEY `idx_domain` (`domain`);

INSERT INTO `system_dict_common`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `parent_code`, `level`, `domain`,
   `sort_order`, `icon`, `css_class`, `extra`, `status`, `is_default`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
SELECT
  `dict_type`, `dict_code`, `dict_name`, `dict_value`, `parent_code`, 1, `domain`,
  `sort_order`, `icon`, NULL, `extra`, `status`, 0, `remark`,
  `created_at`, `updated_at`, `creator`, `updater`, 0
FROM `system_dict_business`
WHERE `deleted` = 0
ON DUPLICATE KEY UPDATE `updated_at` = `system_dict_business`.`updated_at`;

DELETE FROM `system_menu` WHERE `menu_code` = 'admin_system_dict_business';

DELETE FROM `organ_role_permission_ship` WHERE `permission_code` LIKE 'system:dict-biz:%';
DELETE FROM `organ_permission` WHERE `permission_code` LIKE 'system:dict-biz:%';

DROP TABLE `system_dict_business`;

RENAME TABLE `system_dict_common` TO `system_dict`;
