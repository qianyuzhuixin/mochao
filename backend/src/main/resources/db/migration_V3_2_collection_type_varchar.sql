-- ============================================
-- V3.2: t_collection.type 列类型修正
-- 问题: 原为 TINYINT(0/1)，但应用层使用字符串(word/sentence)
-- 修复: 改为 VARCHAR(20)，兼容前端传递的 'word'/'sentence'
-- ============================================

ALTER TABLE t_collection
    MODIFY COLUMN type VARCHAR(20) DEFAULT 'sentence' COMMENT '类型: word-好词 sentence-好句';

-- 已有数据的 type 值转换: 0 → 'word', 1 → 'sentence'
UPDATE t_collection SET type = 'word' WHERE type = '0';
UPDATE t_collection SET type = 'sentence' WHERE type = '1';
