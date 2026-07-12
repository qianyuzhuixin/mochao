package com.mochao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * RestTemplate 配置 — 提供可复用的 RestTemplate Bean，避免每次 AI 调用都创建新实例
 */
@Configuration
public class RestTemplateConfig {

    @Value("${ai.proxy-host:}")
    private String proxyHost;

    @Value("${ai.proxy-port:0}")
    private Integer proxyPort;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && proxyPort > 0) {
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(60000);
        } else {
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(30000);
        }
        return new RestTemplate(factory);
    }
}
