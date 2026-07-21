package com.mochao.module.ai.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AiGenerateDTO {

    private Long novelId;

    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    /** 父级类型（用于上下文链式传递）：volume/act，为空则不传 */
    private String parentType;

    /** 父级ID（卷ID或幕ID），用于获取上级内容作为上下文 */
    private Long parentId;
}
