-- 音乐表添加收藏字段
ALTER TABLE t_music ADD COLUMN favorite TINYINT DEFAULT 0 COMMENT '是否收藏(0=否,1=是)' AFTER duration;
