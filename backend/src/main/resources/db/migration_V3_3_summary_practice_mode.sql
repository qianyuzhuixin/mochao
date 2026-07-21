-- V3.3 新增摘要写作练习模式
-- 在 t_practice_session 表添加 mode、summary_content、self_written_content 字段

ALTER TABLE t_practice_session
    ADD COLUMN mode VARCHAR(20) DEFAULT 'copy' COMMENT '练习模式: copy-1:1抄写 summary-摘要写作',
    ADD COLUMN summary_content TEXT COMMENT '摘要内容',
    ADD COLUMN self_written_content TEXT COMMENT '自写内容';

-- 现有数据默认设为 copy 模式
UPDATE t_practice_session SET mode = 'copy' WHERE mode IS NULL;
