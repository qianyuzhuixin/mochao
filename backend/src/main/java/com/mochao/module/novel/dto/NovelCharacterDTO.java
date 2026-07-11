package com.mochao.module.novel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class NovelCharacterDTO {

    private Long id;

    @NotBlank(message = "人物名称不能为空")
    private String name;

    private String role;
    private String avatar;
    private String appearance;
    private String personality;
    private String background;
    private String relationships;
    private Integer firstAppearance;
    private Integer sortOrder;
}
