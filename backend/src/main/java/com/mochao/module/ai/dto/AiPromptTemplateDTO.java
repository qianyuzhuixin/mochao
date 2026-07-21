package com.mochao.module.ai.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AiPromptTemplateDTO {

    private Long id;

    @NotBlank(message = "功能类型不能为空")
    private String feature;

    private String name;

    @NotBlank(message = "提示词不能为空")
    private String systemPrompt;

    private Boolean isActive;
}
