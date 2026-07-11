package com.mochao.module.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_daily_statistics")
public class DailyStatistics {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate statDate;

    private Integer practiceCount;

    private Integer totalChars;

    private Integer totalDuration;

    private Double avgAccuracy;

    private Double avgSpeed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
