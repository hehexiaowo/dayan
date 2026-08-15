-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 63_knowledge_repo.sql  百炼知识仓库（大雁养老平台 + 每渠道一个知识库）
--
-- 设计要点：
-- 1. knowledge_repo 只存「仓库」本地元数据（名称/归属/百炼远端索引ID）；
--    文档与解析状态以百炼远端为准（ListIndexDocuments 实时代理），
--    本地不建文档表，避免双写不一致 ——「同步管理」的真意。
-- 2. 创建仓库 = 调百炼 CreateIndex + SubmitIndexJob 构建索引，index_id 落库；
--    也支持绑定百炼控制台已建索引（手填 IndexId）。
-- 3. system_config 新增 llm 分组（模型推理 + 知识库管理两套凭据）：
--    - llm.api-key / api-host：模型推理（OpenAI 兼容专属网关，AI 问答用）；
--    - llm.access-key-id / access-key-secret / workspace-id / region：知识库
--      管理 OpenAPI（AccessKey 签名，RAM 子账号需 AliyunBailianDataFullAccess）。
--    敏感项 is_secret=1 走既有脱敏 + 审计机制；种子值留空，由系统配置页填写
--    （真实值绝不入库 git）。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `knowledge_repo` (
  `id`             BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `repo_code`      VARCHAR(50) NOT NULL COMMENT '仓库编码（KB+序号，唯一）',
  `repo_name`      VARCHAR(100) NOT NULL COMMENT '仓库名称（如：大雁养老平台知识库 / xx渠道知识库）',
  `repo_type`      TINYINT NOT NULL DEFAULT 1 COMMENT '归属类型（1=平台大雁养老 2=渠道）',
  `channel_code`   VARCHAR(50) DEFAULT NULL COMMENT '渠道编码（repo_type=2 时关联 channel_info.channel_code）',
  `index_id`       VARCHAR(64) DEFAULT NULL COMMENT '百炼远端索引 ID（CreateIndex 返回 Data.Id）',
  `build_job_id`   VARCHAR(64) DEFAULT NULL COMMENT '建库索引构建任务 ID（SubmitIndexJob 返回 Data.Id，构建完成前为 NULL 之外的值）',
  `description`    VARCHAR(255) DEFAULT NULL COMMENT '仓库描述',
  `doc_count`      INT NOT NULL DEFAULT 0 COMMENT '文档数（以百炼远端为准，sync 时刷新）',
  `status`         TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0=未初始化 1=正常 2=远端异常）',
  `last_sync_at`   DATETIME DEFAULT NULL COMMENT '最近同步时间',
  `sort_order`     INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator`        VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `updater`        VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  `deleted`        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删,1=已删）',
  `deleted_at`     DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repo_code` (`repo_code`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='百炼知识仓库（平台 + 每渠道一个，远端索引元数据）';

-- ---------- system_config：llm 凭据分组（槽位，值由系统配置页填写）----------
INSERT INTO `system_config`
  (`config_group`, `config_key`, `config_value`, `value_type`, `env`, `scope`,
   `config_name`, `description`, `is_secret`, `is_runtime`, `sort_order`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('llm', 'llm.api-key', '', 'string', 'prod', 'global',
   '百炼模型 API-Key', '百炼控制台创建的 API-Key（sk- 开头，专属网关 OpenAI 兼容推理用）。AI 问答与检索重写消费', 1, 1, 10,
   NOW(), NOW(), 'system', 'system', 0),
  ('llm', 'llm.api-host', '', 'string', 'prod', 'global',
   '百炼专属网关域名', '专属网关域名（不含协议与路径，如 llm-xxx.cn-beijing.maas.aliyuncs.com），兼容地址自动拼 /compatible-mode/v1', 0, 1, 20,
   NOW(), NOW(), 'system', 'system', 0),
  ('llm', 'llm.access-key-id', '', 'string', 'prod', 'global',
   '知识库 AccessKey ID', '百炼知识库管理 OpenAPI 访问密钥 ID（RAM 子账号，需 AliyunBailianDataFullAccess 权限并加入业务空间）', 1, 1, 30,
   NOW(), NOW(), 'system', 'system', 0),
  ('llm', 'llm.access-key-secret', '', 'string', 'prod', 'global',
   '知识库 AccessKey Secret', '百炼知识库管理 OpenAPI 访问私钥（敏感，编辑留空保持不变）', 1, 1, 40,
   NOW(), NOW(), 'system', 'system', 0),
  ('llm', 'llm.workspace-id', '', 'string', 'prod', 'global',
   '百炼业务空间 ID', 'WorkspaceId（百炼控制台-业务空间详情查看），知识库管理与文件上传均需要', 0, 1, 50,
   NOW(), NOW(), 'system', 'system', 0),
  ('llm', 'llm.region', 'cn-beijing', 'string', 'prod', 'global',
   '百炼地域', '百炼服务地域（OpenAPI endpoint bailian.{region}.aliyuncs.com）', 0, 1, 60,
   NOW(), NOW(), 'system', 'system', 0),
  ('llm', 'llm.chat-model', 'qwen-plus', 'string', 'prod', 'global',
   'AI 问答模型', '知识库问答使用的模型名（百炼兼容模式，如 qwen-plus / qwen-turbo）', 0, 1, 70,
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ---------- system_menu：资源管理 → 知识仓库 ----------
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_resource_knowledge', '知识仓库', 'admin_resource', 2, '/resource/knowledge', 'resource/knowledge/index', 'knowledge:repo:list', 'Collection', 6, 1, 'admin', 1, '百炼知识仓库管理（平台 + 每渠道一个）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_code` = `menu_code`;
