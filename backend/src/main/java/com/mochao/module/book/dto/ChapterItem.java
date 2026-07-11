package com.mochao.module.book.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterItem {

    /** 章节序号 */
    private Integer index;

    /** 章节标题（如"第一章 楔子"） */
    private String title;

    /** 章节完整内容 */
    private String content;

    /** 字数 */
    private Integer wordCount;

    /** 内容预览（前100字） */
    private String preview;
}
