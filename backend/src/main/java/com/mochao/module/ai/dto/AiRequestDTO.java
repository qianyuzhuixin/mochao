package com.mochao.module.ai.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AiRequestDTO {

    private Long novelId;

    private Long chapterId;

    @NotBlank(message = "选中文本不能为空")
    private String selectedText;

    private String customPrompt;
}
