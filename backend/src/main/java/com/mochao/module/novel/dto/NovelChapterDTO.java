package com.mochao.module.novel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class NovelChapterDTO {

    private Long id;

    private Long outlineId;

    private Integer chapterNumber;

    @NotBlank(message = "章节标题不能为空")
    private String title;

    private String content;

    private String status;
}
