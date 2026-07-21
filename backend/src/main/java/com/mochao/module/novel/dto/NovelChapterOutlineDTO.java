package com.mochao.module.novel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class NovelChapterOutlineDTO {

    private Long id;

    /** 所属幕ID（可选，不填则不关联幕） */
    private Long actId;

    private Integer chapterNumber;

    @NotBlank(message = "章纲标题不能为空")
    private String title;

    private String summary;
    private String detail;
    private String status;
}
