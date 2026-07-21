package com.mochao.module.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_novel_act")
public class NovelAct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long novelId;

    private Long volumeId;

    private Integer actNumber;

    private String title;

    private String outline;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
