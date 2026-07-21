package com.mochao.module.collection.dto;

import lombok.Data;

import java.util.List;

@Data
public class CollectionUpdateDTO {

    private String content;
    private String note;
    private List<String> tags;
}
