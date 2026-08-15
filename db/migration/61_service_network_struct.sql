-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 61_service_network_struct.sql  服务网络结构化升级（服务项目级可精确到房型）
--
-- 背景：service_item.service_network 原为字符串数组 JSON（通配符 "*"、"旅居*"
--       或机构码），管理端为裸 textarea、后端无消费。本次升级为与
--       goods_service_item_rel.network_scope 一致的结构化 JSON：
--         NULL = 该业态全部在营机构
--         {"mode":"custom","parks":[{"parkCode":"PK001","roomTypeCodes":[]}]}
--           = 自选机构范围；roomTypeCodes 空=整馆全部房型，非空=指定房型
--               （随心住类：如"任选一家机构的一间房型免费入住"）
-- 旧数组格式由后端 RightsJson.readNetwork 兼容解析（通配符→全部、机构码→整馆）。
-- =====================================================================

ALTER TABLE `service_item`
  MODIFY COLUMN `service_network` TEXT NULL
    COMMENT '服务网络范围JSON：NULL=该业态全部在营机构；{"mode":"custom","parks":[{"parkCode":"PK001","roomTypeCodes":[]}]}=自选（roomTypeCodes空=该机构全部房型）';

ALTER TABLE `goods_service_item_rel`
  MODIFY COLUMN `network_scope` TEXT NULL
    COMMENT '商品级服务网络收窄JSON：NULL=继承服务项目的网络范围；{"mode":"custom","parks":[...]}=自选（结构同 service_item.service_network）';

-- 存量通配数组（'["*"]' 等）统一归一为 NULL（=业态全部），与读取兼容口径一致
UPDATE `service_item`
  SET `service_network` = NULL
  WHERE `service_network` IS NOT NULL
    AND `service_network` LIKE '[%"*"%]';
