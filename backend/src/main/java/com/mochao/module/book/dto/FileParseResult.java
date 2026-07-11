package com.mochao.module.book.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileParseResult {

    /** 文件名 */
    private String fileName;

    /** 总字数 */
    private Integer totalWords;

    /** 解析出的章节列表 */
    private List<ChapterItem> chapters;
}
