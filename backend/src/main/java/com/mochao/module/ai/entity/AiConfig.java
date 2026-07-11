package com.mochao.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_config")
public class AiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String providerName;

    private String apiUrl;

    private String apiKey;

    private String model;

    private Integer maxTokens;

    private Double temperature;

    private String proxyHost;

    private Integer proxyPort;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
