package com.mochao.module.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_novel_daily_progress")
public class NovelDailyProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long novelId;

    private Long userId;

    private LocalDate progressDate;

    private Integer wordsWritten;

    private Integer chaptersCompleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
