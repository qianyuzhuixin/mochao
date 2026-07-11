-- ============================================
-- 章节分表存储迁移
-- ============================================

-- 1. 建表
CREATE TABLE IF NOT EXISTS t_book_chapter (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    book_id      BIGINT       NOT NULL COMMENT '所属书ID',
    chapter_idx  INT          NOT NULL COMMENT '章节序号(0=前言)',
    title        VARCHAR(200) DEFAULT NULL COMMENT '章节标题',
    content      LONGTEXT     NOT NULL COMMENT '章节内容',
    word_count   INT          DEFAULT 0 COMMENT '字数',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_book_chapter (book_id, chapter_idx),
    FOREIGN KEY (book_id) REFERENCES t_book(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书本章节表';

-- 2. 数据迁移（由应用层执行，此脚本仅建表）
-- 已有 t_book.content 的迁移在应用启动后自动进行，
-- 调用 BookService 的 migrateExistingContent() 即可。
