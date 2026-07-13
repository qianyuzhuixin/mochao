-- 网文榜单快照表
CREATE TABLE IF NOT EXISTS t_ranking_snapshot (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform    VARCHAR(20)   NOT NULL COMMENT '平台: qidian/fanqie/jinjiang/zhihu',
    rank_type   VARCHAR(30)   NOT NULL COMMENT '榜单: month_ticket/recommend/collect/hot_search/new_book',
    rank_no     INT           NOT NULL COMMENT '排名',
    book_name   VARCHAR(200)  NOT NULL COMMENT '书名',
    author      VARCHAR(100)  DEFAULT '' COMMENT '作者',
    category    VARCHAR(50)   DEFAULT '' COMMENT '分类标签',
    word_count  BIGINT        DEFAULT 0 COMMENT '字数',
    hot_value   BIGINT        DEFAULT 0 COMMENT '热度值(月票/推荐票/收藏数)',
    intro       TEXT          COMMENT '简介摘要',
    cover_url   VARCHAR(500)  DEFAULT '' COMMENT '封面URL',
    book_url    VARCHAR(500)  DEFAULT '' COMMENT '原书链接',
    snap_date   DATE          NOT NULL COMMENT '快照日期',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_platform_date (platform, snap_date, rank_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网文榜单快照';
