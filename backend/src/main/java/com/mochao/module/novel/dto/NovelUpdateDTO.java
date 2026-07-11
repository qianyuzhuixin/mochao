package com.mochao.module.novel.dto;

import lombok.Data;

@Data
public class NovelUpdateDTO {

    private String title;
    private String genre;
    private String summary;
    private String cover;
    private String status;
    private Integer targetWords;
}
