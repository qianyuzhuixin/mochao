package com.mochao.module.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel")
public class Novel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String genre;

    private String summary;

    private String cover;

    private String status;

    private Integer targetWords;

    private Integer totalWords;

    private Integer chapterCount;

    private Integer completedChapters;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
