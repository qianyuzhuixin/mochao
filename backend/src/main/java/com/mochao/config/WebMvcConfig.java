package com.mochao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${mochao.upload.music-dir:uploads/music}")
    private String musicDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射音乐文件静态资源：/files/music/xxx → uploads/music/xxx
        registry.addResourceHandler("/files/music/**")
                .addResourceLocations("file:" + musicDir + "/");
    }
}
