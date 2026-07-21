package com.mochao.module.ai.enums;

/**
 * AI 功能类型枚举 — 替代 AiServiceImpl 中的魔法字符串
 *
 * 旧代码散落在 saveLog() 和 switch(dto.getType()) 中：
 *   "optimize", "expand", "condense", "continue",
 *   "polish-dialogue", "predict",
 *   "generate-outline", "generate-character", "generate-worldview", "generate-chapter_outline"
 *
 * 现在统一用枚举，编译器帮你校验，IDE 帮你跳转。
 */
public enum AiFeatureType {

    // === 编辑类功能 ===
    OPTIMIZE("optimize", "优化文本"),
    EXPAND("expand", "扩写文本"),
    CONDENSE("condense", "精简文本"),
    CONTINUE("continue", "续写文本"),
    POLISH_DIALOGUE("polish-dialogue", "优化对话"),
    PREDICT("predict", "预测情节"),

    // === 生成类功能 ===
    GENERATE_OUTLINE("generate-outline", "生成大纲"),
    GENERATE_VOLUME_OUTLINE("generate-volume-outline", "生成卷纲"),
    GENERATE_ACT_OUTLINE("generate-act-outline", "生成幕纲"),
    GENERATE_DETAILED_OUTLINE("generate-detailed-outline", "生成细纲"),
    GENERATE_CHARACTER("generate-character", "生成人物设定"),
    GENERATE_WORLDVIEW("generate-worldview", "生成世界观"),
    GENERATE_CHAPTER_OUTLINE("generate-chapter_outline", "生成章节大纲");

    private final String code;
    private final String description;

    AiFeatureType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从生成类 DTO 的 type 字段映射到枚举
     */
    public static AiFeatureType fromGenerateType(String type) {
        switch (type) {
            case "outline":
                return GENERATE_OUTLINE;
            case "volume_outline":
                return GENERATE_VOLUME_OUTLINE;
            case "act_outline":
                return GENERATE_ACT_OUTLINE;
            case "detailed_outline":
                return GENERATE_DETAILED_OUTLINE;
            case "character":
                return GENERATE_CHARACTER;
            case "worldview":
                return GENERATE_WORLDVIEW;
            case "chapter_outline":
                return GENERATE_CHAPTER_OUTLINE;
            default:
                throw new IllegalArgumentException("不支持的生成类型: " + type);
        }
    }
}
