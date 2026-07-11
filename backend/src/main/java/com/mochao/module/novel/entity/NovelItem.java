package com.mochao.module.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_item")
public class NovelItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long novelId;

    private String name;

    private String category;

    private String appearance;

    private String origin;

    private String attributes;

    private String owner;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
