package com.mochao.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_prompt_template")
public class AiPromptTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，0=系统默认模板 */
    private Long userId;

    /** 功能类型: generate-outline/generate-volume-outline/generate-act-outline/generate-detailed-outline/generate-character/generate-worldview */
    private String feature;

    /** 模板名称 */
    private String name;

    /** 系统提示词 */
    private String systemPrompt;

    /** 是否启用 */
    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
