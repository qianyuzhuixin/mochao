-- ============================================
-- 墨抄(MoChao) 数据库完整建库脚本
-- 合并自: init.sql + migration_ai_config.sql
--        + migration_content_longtext.sql
--        + migration_status_fix.sql
--        + migration_role_fix.sql
-- MySQL 8.0+
-- 执行方式: mysql -u root -p < mochao.sql
-- ============================================

CREATE DATABASE IF NOT EXISTS mochao DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mochao;

-- ============================================
-- 第一部分 建表语句（init.sql）
-- ============================================

-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL COMMENT '用户名',
    email           VARCHAR(100) NOT NULL COMMENT '邮箱',
    password        VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    nickname        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    avatar          VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    signature       VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
    preferred_theme VARCHAR(20)  DEFAULT 'light' COMMENT '偏好主题: light/dark/eye-care',
    font_size       INT          DEFAULT 16 COMMENT '字体大小',
    role            VARCHAR(20)  DEFAULT 'USER' COMMENT '角色: USER-普通用户 ADMIN-管理员',
    status          TINYINT      DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 素材/书籍片段表
-- ============================================
CREATE TABLE IF NOT EXISTS t_book (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL COMMENT '片段标题',
    book_name   VARCHAR(100) DEFAULT NULL COMMENT '所属书名',
    author      VARCHAR(50)  DEFAULT NULL COMMENT '作者',
    category    VARCHAR(30)  DEFAULT NULL COMMENT '分类: 玄幻/都市/武侠/科幻/悬疑/历史/言情',
    tags        VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)',
    content     LONGTEXT     NOT NULL COMMENT '片段正文',
    word_count  INT          DEFAULT 0 COMMENT '字数',
    difficulty  TINYINT      DEFAULT 1 COMMENT '难度: 1-简单 2-中等 3-困难',
    source_type TINYINT      DEFAULT 0 COMMENT '来源: 0-平台内置 1-用户自建',
    creator_id  BIGINT       DEFAULT NULL COMMENT '创建者ID(内置为null)',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 0-下架 1-正常',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_creator (creator_id),
    INDEX idx_category (category),
    INDEX idx_source_type (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='素材/书籍片段表';

-- ============================================
-- 2b. 书本章节表
-- ============================================
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

-- ============================================
-- 4. 练习会话表
-- ============================================
CREATE TABLE IF NOT EXISTS t_practice_session (
    id               BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL COMMENT '用户ID',
    book_id          BIGINT        NOT NULL COMMENT '素材ID',
    chapter_index    INT           DEFAULT NULL COMMENT '章节索引(null=整本练习)',
    chapter_title    VARCHAR(200)  DEFAULT NULL COMMENT '章节标题',
    status           VARCHAR(20)   DEFAULT 'active' COMMENT '状态: active-进行中 paused-暂停 completed-已完成 abandoned-已放弃',
    typed_content    TEXT          DEFAULT NULL COMMENT '已输入内容(断点续练)',
    current_position INT           DEFAULT 0 COMMENT '当前抄写位置(字符索引)',
    total_chars      INT           DEFAULT 0 COMMENT '总字数',
    typed_chars      INT           DEFAULT 0 COMMENT '已输入字数',
    error_count      INT           DEFAULT 0 COMMENT '错误次数',
    start_time       DATETIME      DEFAULT NULL COMMENT '开始时间',
    end_time         DATETIME      DEFAULT NULL COMMENT '结束时间',
    duration         INT           DEFAULT 0 COMMENT '耗时(秒)',
    accuracy         DECIMAL(5,2)  DEFAULT 0.00 COMMENT '正确率(%)',
    speed            DECIMAL(7,2)  DEFAULT 0.00 COMMENT '速度(字/分)',
    score            INT           DEFAULT 0 COMMENT '综合评分',
    created_at       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_book (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习会话表';

-- 4. 每日统计表
-- ============================================
CREATE TABLE IF NOT EXISTS t_daily_statistics (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    stat_date       DATE         NOT NULL COMMENT '统计日期',
    practice_count  INT          DEFAULT 0 COMMENT '练习次数',
    total_chars     INT          DEFAULT 0 COMMENT '总练习字数',
    total_duration  INT          DEFAULT 0 COMMENT '总耗时(秒)',
    avg_accuracy    DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均正确率',
    avg_speed       DECIMAL(7,2) DEFAULT 0.00 COMMENT '平均速度',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- 5. 连续打卡记录表
-- ============================================
CREATE TABLE IF NOT EXISTS t_check_in (
    id           BIGINT   PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL COMMENT '用户ID',
    check_date   DATE     NOT NULL COMMENT '打卡日期',
    streak_days  INT      DEFAULT 1 COMMENT '连续天数',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, check_date),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连续打卡记录表';

-- 6. 管理操作日志表
-- ============================================
CREATE TABLE IF NOT EXISTS t_admin_log (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    admin_id    BIGINT      NOT NULL COMMENT '操作人ID',
    action      VARCHAR(50) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(30) DEFAULT NULL COMMENT '操作对象类型',
    target_id   BIGINT      DEFAULT NULL COMMENT '操作对象ID',
    detail      TEXT        DEFAULT NULL COMMENT '操作详情',
    created_at  DATETIME    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin (admin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理操作日志表';

-- 7. 好词好句收藏表
-- ============================================
CREATE TABLE IF NOT EXISTS t_collection (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    book_id       BIGINT       DEFAULT NULL COMMENT '来源素材ID',
    content       TEXT         NOT NULL COMMENT '收藏的词/句内容',
    type          TINYINT      DEFAULT 1 COMMENT '类型: 0-好词 1-好句',
    context       TEXT         DEFAULT NULL COMMENT '原文上下文',
    note          TEXT         DEFAULT NULL COMMENT '个人笔记/感悟',
    tags          VARCHAR(255) DEFAULT NULL COMMENT '自定义标签(逗号分隔)',
    source_title  VARCHAR(100) DEFAULT NULL COMMENT '来源片段标题',
    source_book   VARCHAR(100) DEFAULT NULL COMMENT '来源书名',
    source_author VARCHAR(50)  DEFAULT NULL COMMENT '来源作者',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好词好句收藏表';

-- 8. 收藏标签表
-- ============================================
CREATE TABLE IF NOT EXISTS t_collection_tag (
    id         BIGINT      PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL COMMENT '用户ID',
    name       VARCHAR(30) NOT NULL COMMENT '标签名称',
    use_count  INT         DEFAULT 0 COMMENT '使用次数',
    created_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_name (user_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏标签表';

-- 9. 小说项目表
-- ============================================
CREATE TABLE IF NOT EXISTS t_novel (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL COMMENT '作者ID',
    title               VARCHAR(100) NOT NULL COMMENT '小说名称',
    genre               VARCHAR(30)  DEFAULT NULL COMMENT '类型: 玄幻/都市/武侠/科幻...',
    summary             TEXT         DEFAULT NULL COMMENT '简介',
    cover               VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    status              VARCHAR(20)  DEFAULT 'draft' COMMENT '状态: draft-草稿 ongoing-连载中 completed-已完结 archived-已弃坑',
    target_words        INT          DEFAULT 1000000 COMMENT '目标字数',
    total_words         INT          DEFAULT 0 COMMENT '已写总字数',
    chapter_count       INT          DEFAULT 0 COMMENT '章节总数',
    completed_chapters  INT          DEFAULT 0 COMMENT '已完成章节数',
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说项目表';

-- 10. 大纲表
-- ============================================
CREATE TABLE IF NOT EXISTS t_novel_outline (
    id         BIGINT   PRIMARY KEY AUTO_INCREMENT,
    novel_id   BIGINT   NOT NULL COMMENT '小说ID',
    content    LONGTEXT DEFAULT NULL COMMENT '大纲内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说大纲表';

-- 11. 世界观表
-- ============================================
CREATE TABLE IF NOT EXISTS t_novel_worldview (
    id         BIGINT   PRIMARY KEY AUTO_INCREMENT,
    novel_id   BIGINT   NOT NULL COMMENT '小说ID',
    content    LONGTEXT DEFAULT NULL COMMENT '世界观内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说世界观表';

-- 12. 人物设定表
-- ============================================
CREATE TABLE IF NOT EXISTS t_novel_character (
    id               BIGINT      PRIMARY KEY AUTO_INCREMENT,
    novel_id         BIGINT      NOT NULL COMMENT '小说ID',
    name             VARCHAR(50) NOT NULL COMMENT '人物姓名',
    role             VARCHAR(20) DEFAULT NULL COMMENT '角色定位: 主角/配角/反派/龙套',
    avatar           VARCHAR(255) DEFAULT NULL COMMENT '人物头像URL',
    appearance       TEXT        DEFAULT NULL COMMENT '外貌描述',
    personality      TEXT        DEFAULT NULL COMMENT '性格特征',
    background       TEXT        DEFAULT NULL COMMENT '背景故事',
    relationships    TEXT        DEFAULT NULL COMMENT '人物关系',
    first_appearance INT         DEFAULT NULL COMMENT '首次出场章节号',
    sort_order       INT         DEFAULT 0 COMMENT '排序序号',
    created_at       DATETIME    DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说人物设定表';

-- 13. 物品设定表
-- ============================================
CREATE TABLE IF NOT EXISTS t_novel_item (
    id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
    novel_id   BIGINT       NOT NULL COMMENT '小说ID',
    name       VARCHAR(100) NOT NULL COMMENT '物品名称',
    category   VARCHAR(20)  DEFAULT NULL COMMENT '分类: 武器/法宝/丹药/功法/材料/其他',
    appearance TEXT         DEFAULT NULL COMMENT '外观描述',
    origin     TEXT         DEFAULT NULL COMMENT '来历',
    attributes TEXT         DEFAULT NULL COMMENT '属性能力',
    owner      VARCHAR(50)  DEFAULT NULL COMMENT '当前持有者',
    sort_order INT          DEFAULT 0 COMMENT '排序序号',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说物品设定表';

-- 14. 章纲表
-- ============================================
CREATE TABLE IF NOT EXISTS t_novel_chapter_outline (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    novel_id       BIGINT       NOT NULL COMMENT '小说ID',
    chapter_number INT          NOT NULL COMMENT '章节号',
    title          VARCHAR(100) DEFAULT NULL COMMENT '章节标题',
    summary        TEXT         DEFAULT NULL COMMENT '章纲摘要',
    detail         TEXT         DEFAULT NULL COMMENT '细纲',
    status         VARCHAR(20)  DEFAULT 'draft' COMMENT '状态: draft-草稿 published-已完成',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_novel_chapter (novel_id, chapter_number),
    INDEX idx_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说章纲表';

-- 15. 章节内容表
-- ============================================
CREATE TABLE IF NOT EXISTS t_novel_chapter (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    novel_id       BIGINT       NOT NULL COMMENT '小说ID',
    outline_id     BIGINT       DEFAULT NULL COMMENT '关联章纲ID',
    chapter_number INT          NOT NULL COMMENT '章节号',
    title          VARCHAR(100) DEFAULT NULL COMMENT '章节标题',
    content        LONGTEXT     DEFAULT NULL COMMENT '章节正文',
    word_count     INT          DEFAULT 0 COMMENT '字数',
    status         VARCHAR(20)  DEFAULT 'draft' COMMENT '状态: draft-草稿 published-已完成',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_novel_chapter (novel_id, chapter_number),
    INDEX idx_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说章节内容表';

-- 16. 每日写作进度表
-- ============================================
CREATE TABLE IF NOT EXISTS t_novel_daily_progress (
    id                 BIGINT   PRIMARY KEY AUTO_INCREMENT,
    novel_id           BIGINT   NOT NULL COMMENT '小说ID',
    user_id            BIGINT   NOT NULL COMMENT '用户ID',
    progress_date      DATE     NOT NULL COMMENT '日期',
    words_written      INT      DEFAULT 0 COMMENT '当日写作字数',
    chapters_completed INT      DEFAULT 0 COMMENT '当日完成章节数',
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_novel_date (novel_id, progress_date),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说每日写作进度表';

-- 17. AI使用记录表
-- ============================================
CREATE TABLE IF NOT EXISTS t_ai_usage_log (
    id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL COMMENT '用户ID',
    novel_id        BIGINT      DEFAULT NULL COMMENT '小说ID',
    chapter_id      BIGINT      DEFAULT NULL COMMENT '章节ID',
    feature         VARCHAR(30) NOT NULL COMMENT '功能类型',
    input_text      TEXT        DEFAULT NULL COMMENT '输入内容',
    output_text     TEXT        DEFAULT NULL COMMENT 'AI输出内容',
    context_summary TEXT        DEFAULT NULL COMMENT '上下文摘要',
    tokens_used     INT         DEFAULT 0 COMMENT '消耗token数',
    adopted         TINYINT     DEFAULT 0 COMMENT '是否采纳: 0-否 1-是',
    created_at      DATETIME    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id, created_at),
    INDEX idx_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI使用记录表';

-- 18. AI配置表
-- ============================================
CREATE TABLE IF NOT EXISTS t_ai_config (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    provider_name   VARCHAR(50)  NOT NULL COMMENT '提供商名称',
    api_url         VARCHAR(255) NOT NULL COMMENT 'API地址',
    api_key         VARCHAR(255) NOT NULL COMMENT 'API密钥',
    model           VARCHAR(50)  NOT NULL COMMENT '模型名称',
    max_tokens      INT          DEFAULT 2000 COMMENT '最大Token数',
    temperature     DOUBLE       DEFAULT 0.8 COMMENT '温度参数',
    proxy_host      VARCHAR(100) DEFAULT NULL COMMENT '代理主机',
    proxy_port      INT          DEFAULT NULL COMMENT '代理端口',
    is_active       TINYINT      DEFAULT 0 COMMENT '是否激活: 0-否 1-是',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI配置表';


-- ============================================
-- 第二部分 种子数据
-- ============================================

-- 管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO t_user (username, email, password, nickname, role, status) VALUES
('admin', 'admin@mochao.com', '$2a$10$4v6KwwBCV10vJTuekGYNQuPn57kaG3q8aIwbdvRMwowWDgmPHXaji', '管理员', 'ADMIN', 1);

-- 内置书库素材
INSERT INTO t_book (title, book_name, author, category, tags, content, word_count, difficulty, source_type, status) VALUES
('剑九黄', '雪中悍刀行', '烽火戏诸侯', '玄幻', '剑客,江湖,豪迈', '老黄背着剑匣，走入了北莽。他说，这一剑，叫六千里。他说这六千里路，他走了三十年。他说，他这辈子只出了一剑。但这天下，都知道这一剑。\n\n剑九，六千里。这是他走遍天下的路。他是条狗，一条替世子殿下挡了三十年风雨的老狗。老狗也有咬人的时候。当那天上的剑仙如雨落，当那地上的大宗师纷纷出手，老黄拔出了他那把已经锈蚀的铁剑。\n\n黄阵图，剑九黄。这个天下，没有人能让他拔第二次剑。', 168, 2, 0, 1),
('齐静春', '剑来', '烽火戏诸侯', '玄幻', '文圣,道理,守护', '齐静春，这个在小镇上住了很多年的读书人，看上去一点都不像传说中的圣人。他喜欢喝黄酒，喜欢坐在门槛上晒太阳，喜欢跟小镇的孩子们讲一些似是而非的道理。\n\n没有人知道，这座小镇，是这位读书人最后的底线。也没有人知道，那些高高在上的神仙，为何要忌惮一个看上去平平无奇的读书人。\n\n齐先生笑着说："天下道理千万，我认的死理只有一条——护住身后这些人。"\n\n然后他死了。为了让小镇里那些普通人，能够继续过着普通的日子。', 172, 2, 0, 1),
('许七安', '大奉打更人', '卖报小郎君', '玄幻', '打更人,破案,热血', '打更人，大奉王朝最令人忌惮的职业。他们身着皂衣，腰悬铜锣，夜巡京城，看似只是报时打更的差役，实则手握监察百官、先斩后奏之权。\n\n许七安坐在司天监的屋顶上，看着京城的万家灯火。他刚破了一桩大案，本该高兴，但心底却沉甸甸的。\n\n"这世上的真相，就像京城的雾，"他自言自语，"你以为拨开了一层就够了，可后面还有一层，一层又一层，无穷无尽。"\n\n他摸了摸腰间的铜锣，那冰凉的触感提醒着他——他是打更人，他的职责是敲响警钟，让那些沉浸在美梦中的人醒来。', 169, 2, 0, 1),
('萧炎', '斗破苍穹', '天蚕土豆', '玄幻', '少年,逆袭,热血', '萧炎站在悬崖边缘，望着远方连绵的山脉，心中涌起一股难以言喻的感觉。\n\n"三年之约，今日该了结了。"他低声说道，语气中带着几分坚定。\n\n三年前，他被云岚宗逐出家族，被未婚妻当众退婚，被所有人视为废物。三年后，他带着一身惊天修为归来。\n\n风起云涌，他的衣袍猎猎作响。身后，药老悬浮在骨灵冷火之中，看着自己的弟子，嘴角微微上扬。\n\n"小子，怕吗？"\n\n萧炎笑了："怕？从走出乌坦城那天起，我就没怕过。"\n\n他抬脚迈出，一步踏空，身形如流星般掠向云岚宗。', 186, 1, 0, 1),
('韩立', '凡人修仙传', '忘语', '仙侠', '修仙,谨慎,炼丹', '韩立屏住呼吸，小心翼翼地将灵草投入炼丹炉中。这是他第三次尝试炼制筑基丹，前两次都以失败告终。\n\n修仙界弱肉强食，一步落后便是万劫不复。他深知自己资质平庸，能走到今天，全靠一个"稳"字。\n\n炉中丹液翻滚，灵气氤氲。他凝神聚气，不敢有丝毫懈怠。手上的法诀变换不停，每一道灵力都精准无比。\n\n"成了！"他心中暗喜，却面色不变，依旧保持着冷静。丹炉中一颗圆润的丹药缓缓成型，散发出淡淡药香。\n\n韩立没有急着取出丹药，而是默默收起法诀，又等了片刻，确认再无变数后，才小心翼翼地将丹药收入玉瓶。', 204, 2, 0, 1),
('范闲', '庆余年', '猫腻', '架空', '穿越,权谋,少年', '范闲站在庆庙的门槛前，看着庙里那尊泥塑神像，忽然笑了。\n\n"我这个人，从来不信命。"他说。\n\n他是带着另一个世界的记忆来到这里的。四岁练功，六岁杀人，十二岁写下"红楼梦"，十六岁进京。每一步都像是被一只无形的手推着走。\n\n但他偏要走自己的路。\n\n京都的权谋如同一张大网，父亲、皇帝、长公主、宰相……每个人都在下棋，而他，是那颗最不受控的棋子。\n\n"我范闲，宁可掀翻棋盘，也不做别人手中的子。"\n\n他转身离去，背影潇洒。少年的衣袍在风中翻飞，像是某种宣言。', 197, 2, 0, 1),
('宁缺', '将夜', '猫腻', '玄幻', '少年,复仇,书院', '宁缺坐在书院后山的石阶上，看着远处长安城的万家灯火，一言不发。\n\n他杀过人，偷过东西，说过谎，在边塞军中摸爬滚打多年。他不是什么好人，也不打算做好人。他只想活下去，然后找到当年那些人，把他们一个一个杀掉。\n\n但书院的老头子说，他身上有浩然气。那个邋遢酒鬼说，他可以修大道。那个绝美的女子说，她愿意嫁给他。\n\n宁缺摸了摸怀里的那把短刀，心想：这个世道，连活着都这么难，你们跟我说什么大道？\n\n"先活下去，"他对自己说，"其他的，以后再说。"', 206, 3, 0, 1),
('序列', '诡秘之主', '爱潜水的乌贼', '奇幻', '克苏鲁,蒸汽朋克,神秘学', '在这个蒸汽与机械的时代，在维多利亚风格的伦敦雾中，隐藏着另一个世界。\n\n序列。从九到零，每一条序列都通向一个不同的神。占卜家、读心者、学徒、偷盗者……每饮下一份魔药，就向神性更近一步，但也离人性更远一步。\n\n克莱恩坐在书桌前，翻阅着那份古老的手稿。煤油灯的火焰在他眼中跳动，映出一种不属于这个世界的智慧。\n\n"超凡之路，从来不是馈赠，而是代价。"他在笔记本上写道。\n\n窗外，蒸汽火车的汽笛声划破夜空。远处教堂的钟声敲响十二下。他合上笔记本，走向那扇半开的门——门后，是另一个维度的星空。', 218, 3, 0, 1);


-- ============================================
-- 第三部分 迁移修复（已部署旧数据库兼容）
-- 以下 SQL 使用 IF EXISTS 守卫，对新建库无影响
-- 仅用于修复旧版数据库的类型不一致问题
-- ============================================

-- --------------------------------------------
-- 3.1 角色字段修复 (migration_role_fix.sql)
--    将 t_user.role 从 TINYINT 迁移为 VARCHAR
-- --------------------------------------------
-- 仅当 role 列不是 varchar 时才执行
-- 步骤1: 新增临时列
SET @role_is_varchar = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'mochao' AND TABLE_NAME = 't_user'
    AND COLUMN_NAME = 'role' AND DATA_TYPE = 'varchar');

SET @sql_role_add = IF(@role_is_varchar = 0,
    'ALTER TABLE t_user ADD COLUMN role_new VARCHAR(20) DEFAULT ''USER'' COMMENT ''角色: USER-普通用户 ADMIN-管理员''',
    'SELECT ''t_user.role 已是 VARCHAR，跳过迁移'' AS info');
PREPARE stmt FROM @sql_role_add; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤2: 转换旧值
SET @sql_role_convert = IF(@role_is_varchar = 0,
    'UPDATE t_user SET role_new = CASE WHEN role = 1 THEN ''ADMIN'' ELSE ''USER'' END',
    'SELECT ''跳过''');
PREPARE stmt FROM @sql_role_convert; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤3: 删除旧列
SET @sql_role_drop = IF(@role_is_varchar = 0,
    'ALTER TABLE t_user DROP COLUMN role',
    'SELECT ''跳过''');
PREPARE stmt FROM @sql_role_drop; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤4: 重命名新列
SET @sql_role_rename = IF(@role_is_varchar = 0,
    'ALTER TABLE t_user CHANGE COLUMN role_new role VARCHAR(20) DEFAULT ''USER'' COMMENT ''角色: USER-普通用户 ADMIN-管理员''',
    'SELECT ''跳过''');
PREPARE stmt FROM @sql_role_rename; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------
-- 3.2 content 字段修复 (migration_content_longtext.sql)
--    将 t_book.content 从 TEXT 扩展为 LONGTEXT
-- --------------------------------------------
SET @content_is_text = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'mochao' AND TABLE_NAME = 't_book'
    AND COLUMN_NAME = 'content' AND DATA_TYPE = 'text');

SET @sql_content = IF(@content_is_text > 0,
    'ALTER TABLE t_book MODIFY COLUMN content LONGTEXT NOT NULL COMMENT ''片段正文''',
    'SELECT ''t_book.content 已是 LONGTEXT，跳过'' AS info');
PREPARE stmt FROM @sql_content; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --------------------------------------------
-- 3.3 status 字段修复 (migration_status_fix.sql)
--    将小说相关表 + 练习表的 status 从 TINYINT 改为 VARCHAR(20)
-- --------------------------------------------

-- t_novel.status
SET @novel_status_is_varchar = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'mochao' AND TABLE_NAME = 't_novel'
    AND COLUMN_NAME = 'status' AND DATA_TYPE = 'varchar');

SET @sql_novel_status = IF(@novel_status_is_varchar = 0,
    'ALTER TABLE t_novel MODIFY COLUMN status VARCHAR(20) DEFAULT ''draft'' COMMENT ''状态: draft-草稿 ongoing-连载中 completed-已完结 archived-已弃坑''',
    'SELECT ''t_novel.status 已是 VARCHAR，跳过'' AS info');
PREPARE stmt FROM @sql_novel_status; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_novel_chapter_outline.status
SET @outline_status_is_varchar = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'mochao' AND TABLE_NAME = 't_novel_chapter_outline'
    AND COLUMN_NAME = 'status' AND DATA_TYPE = 'varchar');

SET @sql_outline_status = IF(@outline_status_is_varchar = 0,
    'ALTER TABLE t_novel_chapter_outline MODIFY COLUMN status VARCHAR(20) DEFAULT ''draft'' COMMENT ''状态: draft-草稿 published-已完成''',
    'SELECT ''t_novel_chapter_outline.status 已是 VARCHAR，跳过'' AS info');
PREPARE stmt FROM @sql_outline_status; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_novel_chapter.status
SET @chapter_status_is_varchar = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'mochao' AND TABLE_NAME = 't_novel_chapter'
    AND COLUMN_NAME = 'status' AND DATA_TYPE = 'varchar');

SET @sql_chapter_status = IF(@chapter_status_is_varchar = 0,
    'ALTER TABLE t_novel_chapter MODIFY COLUMN status VARCHAR(20) DEFAULT ''draft'' COMMENT ''状态: draft-草稿 published-已完成''',
    'SELECT ''t_novel_chapter.status 已是 VARCHAR，跳过'' AS info');
PREPARE stmt FROM @sql_chapter_status; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- t_practice_session.status
SET @session_status_is_varchar = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'mochao' AND TABLE_NAME = 't_practice_session'
    AND COLUMN_NAME = 'status' AND DATA_TYPE = 'varchar');

SET @sql_session_status = IF(@session_status_is_varchar = 0,
    'ALTER TABLE t_practice_session MODIFY COLUMN status VARCHAR(20) DEFAULT ''active'' COMMENT ''状态: active-进行中 paused-暂停 completed-已完成 abandoned-已放弃''',
    'SELECT ''t_practice_session.status 已是 VARCHAR，跳过'' AS info');
PREPARE stmt FROM @sql_session_status; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ============================================
-- 12. 背景音乐表
-- ============================================
DROP TABLE IF EXISTS t_music;
CREATE TABLE t_music (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id     BIGINT          NOT NULL                 COMMENT '用户ID',
    title       VARCHAR(100)    NOT NULL                 COMMENT '曲目标题',
    artist      VARCHAR(100)    DEFAULT ''               COMMENT '艺术家',
    file_name   VARCHAR(255)    NOT NULL                 COMMENT '存储文件名(UUID)',
    file_path   VARCHAR(500)    NOT NULL                 COMMENT '文件路径(相对music-dir)',
    file_size   BIGINT          DEFAULT 0                COMMENT '文件大小(字节)',
    duration    INT             DEFAULT 0                COMMENT '时长(秒)',
    favorite    TINYINT         DEFAULT 0                COMMENT '是否收藏(0=否,1=是)',
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_music_user_id (user_id),
    INDEX idx_music_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='背景音乐';

-- ============================================
-- 完成
-- ============================================
SELECT '墨抄数据库初始化完成！' AS message;
