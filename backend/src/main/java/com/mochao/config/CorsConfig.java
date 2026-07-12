package com.mochao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /** 生产环境允许的域名（逗号分隔），可通过环境变量 CORS_ALLOWED_ORIGINS 配置 */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 开发环境允许 localhost，生产环境从配置读取
        List<String> origins;
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            origins = Arrays.asList(allowedOrigins.split(","));
        } else if ("dev".equals(activeProfile)) {
            origins = Arrays.asList(
                    "http://localhost:8081",
                    "http://localhost:8082",
                    "http://127.0.0.1:8081",
                    "http://127.0.0.1:8082"
            );
        } else {
            // 生产环境未配置时拒绝所有跨域请求
            origins = List.of();
        }

        if (!origins.isEmpty()) {
            config.setAllowedOriginPatterns(origins);
        }
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
