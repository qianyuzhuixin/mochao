package com.mochao.module.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String bookName;

    private String author;

    private String category;

    private String tags;

    private String content;

    private Integer wordCount;

    private Integer difficulty;

    private Integer sourceType;

    private Long creatorId;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
