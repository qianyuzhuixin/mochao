package com.mochao.module.ai.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AiConfigDTO {

    private Long id;

    @NotBlank(message = "提供商名称不能为空")
    private String providerName;

    @NotBlank(message = "API地址不能为空")
    private String apiUrl;

    @NotBlank(message = "API密钥不能为空")
    private String apiKey;

    @NotBlank(message = "模型名称不能为空")
    private String model;

    private Integer maxTokens = 2000;

    private Double temperature = 0.8;

    private String proxyHost;

    private Integer proxyPort;
}
