package com.mochao.module.practice.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PracticeStartDTO {

    @NotNull(message = "书籍ID不能为空")
    private Long bookId;

    /** 章节索引（null=整本练习） */
    private Integer chapterIndex;

    /** 练习模式: copy-1:1抄写 summary-摘要写作，默认 copy */
    private String mode;
}
