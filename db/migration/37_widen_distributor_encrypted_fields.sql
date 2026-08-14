-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- 37_widen_distributor_encrypted_fields.sql
-- 背景：《项目开发规范》5.1 要求身份证/银行账号 AES-256-GCM 加密存储，
-- 密文 = Base64( IV(12B) || ciphertext || GCM-Tag(16B) )，18 位身份证 → 64 字符起步，
-- varchar(20)/(50) 在启用加密后必然 truncation（equity_use_person 已在 36 号踩过同样的坑）。
-- 本文件仅做列宽预留（VARCHAR 放大安全、不丢数据），使 schema 与规范的加密要求对齐；
-- distributor 域 PII 字段的代码侧加密（含存量数据迁移）属于行为变更，需单独评审后实施，
-- 在未启用代码加密前，本列按明文存储亦完全兼容。

ALTER TABLE distributor_info
    MODIFY COLUMN id_card VARCHAR(128) DEFAULT NULL
    COMMENT '身份证号（个人；预留 AES-256-GCM 密文宽度，密文为 Base64(IV||ciphertext||tag)）',
    MODIFY COLUMN bank_account VARCHAR(128) DEFAULT NULL
    COMMENT '银行账号（预留 AES-256-GCM 密文宽度，密文为 Base64(IV||ciphertext||tag)）';
