package com.mochao.module.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_worldview")
public class NovelWorldview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long novelId;

    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
