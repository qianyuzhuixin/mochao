package com.mochao.module.collection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_collection_tag")
public class CollectionTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private Integer useCount;

    private LocalDateTime createdAt;
}
