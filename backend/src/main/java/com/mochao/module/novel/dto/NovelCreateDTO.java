package com.mochao.module.novel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class NovelCreateDTO {

    @NotBlank(message = "小说标题不能为空")
    private String title;

    private String genre;

    private String summary;

    private String cover;

    private Integer targetWords;
}
