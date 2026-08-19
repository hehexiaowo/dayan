SET NAMES utf8mb4;
-- =====================================================================
-- 90_system_knowledge_repo_index_config.sql  知识仓库索引配置外置
--
-- 切分方式/向量模型/重排/改写/召回参数存 config_json（JSON 字符串），
-- 懒建库建库时应用；已建库仅检索参数（denseTopK/sparseTopK/rerankMinScore）可改。
-- 存量仓库 config_json 为空 = 使用百炼默认（智能切分）。
-- =====================================================================
ALTER TABLE `system_knowledge_repo`
  ADD COLUMN `config_json` VARCHAR(2000) NULL
  COMMENT '索引配置 JSON（切分方式/向量模型/重排/改写/召回参数；懒建库建库时应用，已建库仅检索参数可改）'
  AFTER `description`;
