package com.mochao.module.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_character")
public class NovelCharacter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long novelId;

    private String name;

    private String role;

    private String avatar;

    private String appearance;

    private String personality;

    private String background;

    private String relationships;

    private Integer firstAppearance;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
