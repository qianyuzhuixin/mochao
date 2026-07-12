package com.mochao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 文件静态资源映射已移除，改为 FileController 从 MinIO 流式代理。
 * 保留此类用于未来的 MVC 自定义配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // 文件访问由 FileController 处理（MinIO 流式代理）
}
