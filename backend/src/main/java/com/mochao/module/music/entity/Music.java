package com.mochao.module.music.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_music")
public class Music {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String artist;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private Integer duration;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
