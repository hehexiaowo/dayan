-- 36_widen_encrypted_id_card.sql
-- 修复：equity_use_person.use_person_id_card 原 varchar(20) 按明文身份证长度建表，
-- 但 EquityUsePersonServiceImpl.encryptIdCard 用 AES-256-GCM 加密后存储。
-- 密文 = Base64( IV(12B) || ciphertext || GCM-Tag(16B) )，
-- 18 位身份证明文 → 46 字节 → Base64 编码 64 字符，varchar(20) 放不下导致写入 truncation。
-- （client 端"新增权益人/激活建占位人"首次走加密写入路径触发，写操作全部 500）
-- 扩至 varchar(128) 容纳密文 + 余量。
-- 注：扩列是安全的（VARCHAR 放大不丢数据）；加密是《项目开发规范》v1.1 要求，不可退明文。

ALTER TABLE equity_use_person
    MODIFY COLUMN use_person_id_card VARCHAR(128) NULL
    COMMENT '使用人身份证号（AES-256-GCM 密文，Base64(IV||ciphertext||tag)）';
