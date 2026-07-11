package com.mochao.module.collection.dto;

import lombok.Data;

@Data
public class CollectionQueryDTO {

    private String type;
    private Long bookId;
    private String tag;
    private String keyword;
    private Integer page = 1;
    private Integer size = 20;
}
