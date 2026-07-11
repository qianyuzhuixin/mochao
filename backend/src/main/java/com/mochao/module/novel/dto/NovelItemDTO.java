package com.mochao.module.novel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class NovelItemDTO {

    private Long id;

    @NotBlank(message = "物品名称不能为空")
    private String name;

    private String category;
    private String appearance;
    private String origin;
    private String attributes;
    private String owner;
    private Integer sortOrder;
}
