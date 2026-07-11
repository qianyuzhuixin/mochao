package com.mochao.module.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_chapter")
public class NovelChapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long novelId;

    private Long outlineId;

    private Integer chapterNumber;

    private String title;

    private String content;

    private Integer wordCount;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
