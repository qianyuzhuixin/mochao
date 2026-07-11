package com.mochao.module.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_check_in")
public class CheckIn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate checkDate;

    private Integer streakDays;

    private LocalDateTime createdAt;
}
