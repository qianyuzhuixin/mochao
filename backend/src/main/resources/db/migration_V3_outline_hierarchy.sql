-- ============================================
-- 墨抄 V3: 四级大纲体系 + AI提示词模板
-- 大纲(总纲) → 卷纲 → 幕 → 细纲/章纲
-- ============================================

-- 19. 卷表（一卷包含多幕）
CREATE TABLE IF NOT EXISTS t_novel_volume (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    novel_id        BIGINT       NOT NULL COMMENT '小说ID',
    volume_number   INT          NOT NULL COMMENT '卷序号',
    title           VARCHAR(100) DEFAULT NULL COMMENT '卷标题',
    outline         LONGTEXT     DEFAULT NULL COMMENT '卷纲内容',
    sort_order      INT          DEFAULT 0 COMMENT '排序序号',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_novel_volume (novel_id, volume_number),
    INDEX idx_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说卷表';

-- 20. 幕表（一幕包含多个章/细纲）
CREATE TABLE IF NOT EXISTS t_novel_act (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    novel_id        BIGINT       NOT NULL COMMENT '小说ID',
    volume_id       BIGINT       NOT NULL COMMENT '所属卷ID',
    act_number      INT          NOT NULL COMMENT '幕序号',
    title           VARCHAR(100) DEFAULT NULL COMMENT '幕标题',
    outline         LONGTEXT     DEFAULT NULL COMMENT '幕纲内容',
    sort_order      INT          DEFAULT 0 COMMENT '排序序号',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_volume_act (volume_id, act_number),
    INDEX idx_novel (novel_id),
    INDEX idx_volume (volume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说幕表';

-- 章纲表扩展: 关联幕ID
-- 使用存储过程安全添加列（避免重复执行报错）
DROP PROCEDURE IF EXISTS add_act_id_if_missing;
DELIMITER $$
CREATE PROCEDURE add_act_id_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'mochao'
        AND TABLE_NAME = 't_novel_chapter_outline'
        AND COLUMN_NAME = 'act_id'
    ) THEN
        ALTER TABLE t_novel_chapter_outline
        ADD COLUMN act_id BIGINT DEFAULT NULL COMMENT '所属幕ID' AFTER novel_id,
        ADD INDEX idx_act (act_id);
    END IF;
END$$
DELIMITER ;
CALL add_act_id_if_missing();
DROP PROCEDURE IF EXISTS add_act_id_if_missing;

-- 21. AI提示词模板表
CREATE TABLE IF NOT EXISTS t_ai_prompt_template (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    feature         VARCHAR(50)  NOT NULL COMMENT '功能类型: generate-outline/generate-volume-outline/generate-act-outline/generate-detailed-outline/generate-character/generate-worldview',
    name            VARCHAR(100) DEFAULT NULL COMMENT '模板名称',
    system_prompt   TEXT         NOT NULL COMMENT '系统提示词',
    is_active       TINYINT      DEFAULT 1 COMMENT '是否启用: 0-否 1-是',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_feature (user_id, feature),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI提示词模板表';

-- 插入默认提示词模板（user_id=0 表示系统默认，所有用户共享）
INSERT INTO t_ai_prompt_template (user_id, feature, name, system_prompt, is_active) VALUES
(0, 'generate-outline', '系统默认-大纲生成',
 '你是一位资深的网文大纲策划专家，精通各类网络小说流派（玄幻、都市、修仙、科幻、悬疑、历史、言情等）。\n请根据提供的小说基本信息（书名、类型、简介）和用户的具体要求，直接为这本小说生成完整的故事大纲正文。\n\n输出要求：\n1. 使用 Markdown 格式输出\n2. 不要添加任何开场白（如\"好的，根据您的要求\"）、结束语、总结性陈述或解释性语句\n3. 只输出大纲正文\n\n内容要求：\n1. 先进行故事核心设定（主线冲突、世界观定位、主角成长路线）\n2. 规划主要情节节点（开端、发展、转折、高潮、结局）\n3. 设计分卷结构（每卷应有独立主题和任务）\n4. 注意网文节奏感：每3-5章一个小高潮，每卷一个大高潮\n5. 人物关系网和伏笔铺设建议', 1),

(0, 'generate-volume-outline', '系统默认-卷纲生成',
 '你是一位网文卷纲策划专家。\n请根据提供的小说大纲和用户要求，直接为指定卷生成详细的卷纲正文。\n\n输出要求：\n1. 使用 Markdown 格式输出\n2. 不要添加任何开场白、结束语、总结性陈述或解释性语句\n3. 只输出卷纲正文\n\n内容要求：\n1. 明确本卷的主题和任务（承上启下的作用）\n2. 规划本卷的幕结构（每幕的核心冲突和情感走向）\n3. 设计关键情节和转折点\n4. 控制节奏：开局吸引→发展推进→小高潮→铺垫下一卷\n5. 标注需要回收和新增的伏笔', 1),

(0, 'generate-act-outline', '系统默认-幕纲生成',
 '你是一位网文幕纲策划专家。\n请根据提供的小说大纲、卷纲和用户要求，直接为指定幕生成详细的幕纲正文。\n\n输出要求：\n1. 使用 Markdown 格式输出\n2. 不要添加任何开场白、结束语、总结性陈述或解释性语句\n3. 只输出幕纲正文\n\n内容要求：\n1. 明确本幕的核心冲突和情感主题\n2. 规划本幕的场景序列（每个场景的功能：推进/展示/伏笔）\n3. 设计具体的章节分布建议（每章要完成什么任务）\n4. 标注场景之间的衔接和情绪曲线\n5. 关键对话和动作场面的要点提示', 1),

(0, 'generate-detailed-outline', '系统默认-细纲生成',
 '你是一位网文细纲策划专家。\n请根据提供的小说大纲、卷纲、幕纲和用户要求，直接为指定范围生成详细的章节细纲正文。\n\n输出要求：\n1. 使用 Markdown 格式输出\n2. 不要添加任何开场白、结束语、总结性陈述或解释性语句\n3. 只输出细纲正文\n\n内容要求：\n1. 每章标注：核心任务、情感目标、信息揭示\n2. 开头钩子设计（吸引读者继续阅读的悬念/冲突/金句）\n3. 每章2-3个场景分解，每个场景标注功能和字数建议\n4. 关键对话要点和动作场面的节奏控制\n5. 章节结尾钩子（预告/悬念/情绪余韵）', 1),

(0, 'generate-character', '系统默认-人物设定',
 '你是一位人物设定专家。请根据提供的信息生成一个完整的人物设定，包括外貌、性格、背景、关系等。', 1),

(0, 'generate-worldview', '系统默认-世界观设定',
 '你是一位世界观架构专家。请根据提供的信息生成一个完整的世界观设定，包括力量体系、地理格局、历史背景、势力分布等。', 1);
