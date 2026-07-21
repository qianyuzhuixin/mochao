package com.mochao.module.novel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class NovelActDTO {

    private Long id;

    @NotNull(message = "所属卷不能为空")
    private Long volumeId;

    private Integer actNumber;

    @NotBlank(message = "幕标题不能为空")
    private String title;

    /** 幕纲内容 */
    private String outline;

    private Integer sortOrder;
}
