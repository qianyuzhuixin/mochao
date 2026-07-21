package com.mochao.module.collection.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class CollectionCreateDTO {

    private Long bookId;

    @NotBlank(message = "收藏内容不能为空")
    private String content;

    private String type;

    private String context;

    private String note;

    private List<String> tags;

    private String sourceTitle;

    private String sourceBook;

    private String sourceAuthor;
}
