package com.mochao.module.book.dto;

import lombok.Data;

@Data
public class BookQueryDTO {

    private String category;
    private String difficulty;
    private String keyword;
    private Integer sourceType = 0; // 默认查内置书库
    private Integer page = 1;
    private Integer size = 20;
}
