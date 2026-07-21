package com.mochao.module.novel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class NovelVolumeDTO {

    private Long id;

    private Integer volumeNumber;

    @NotBlank(message = "卷标题不能为空")
    private String title;

    /** 卷纲内容 */
    private String outline;

    private Integer sortOrder;
}
