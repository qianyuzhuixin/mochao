package com.mochao.module.collection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_collection")
public class Collection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    private String content;

    private String type;

    private String context;

    private String note;

    private String tags;

    private String sourceTitle;

    private String sourceBook;

    private String sourceAuthor;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
