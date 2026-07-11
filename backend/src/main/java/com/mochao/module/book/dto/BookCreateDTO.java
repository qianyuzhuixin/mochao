package com.mochao.module.book.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class BookCreateDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String bookName;

    private String author;

    private String category;

    private String tags;

    private String content;

    /** 前端已解析的章节列表（文件导入时传入，避免后端重复拆分） */
    private List<ChapterItem> chapters;

    private String difficulty;
}
