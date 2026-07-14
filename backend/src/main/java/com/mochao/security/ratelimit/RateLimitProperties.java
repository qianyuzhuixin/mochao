package com.mochao.security.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流配置 — 通过 application.yml 的 rate-limit 前缀配置
 *
 * 开发环境建议放宽或直接 enabled=false，生产环境保持严格
 *
 * 示例配置:
 * rate-limit:
 *   enabled: true
 *   ai-capacity: 10        # AI 接口每分钟每用户最大请求数
 *   auth-capacity: 20      # 认证接口每分钟每IP最大请求数
 *   general-capacity: 60   # 通用接口每分钟每用户最大请求数
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /** 是否启用限流（开发环境可设为 false 跳过） */
    private boolean enabled = true;

    /** AI 接口每分钟每用户最大请求数 */
    private int aiCapacity = 10;

    /** 认证接口每分钟每IP最大请求数 */
    private int authCapacity = 20;

    /** 通用接口每分钟每用户最大请求数 */
    private int generalCapacity = 60;
}
