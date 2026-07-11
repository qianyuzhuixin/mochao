package com.mochao.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_usage_log")
public class AiUsageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long novelId;

    private Long chapterId;

    private String feature;

    private String inputText;

    private String outputText;

    private String contextSummary;

    private Integer tokensUsed;

    private Boolean adopted;

    private LocalDateTime createdAt;
}
