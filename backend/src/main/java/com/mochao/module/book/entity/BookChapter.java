package com.mochao.module.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_book_chapter")
public class BookChapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属书ID */
    private Long bookId;

    /** 章节序号(0=前言) */
    private Integer chapterIdx;

    /** 章节标题 */
    private String title;

    /** 章节内容 */
    private String content;

    /** 字数 */
    private Integer wordCount;

    private LocalDateTime createdAt;
}
