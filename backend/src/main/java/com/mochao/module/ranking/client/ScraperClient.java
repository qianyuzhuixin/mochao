package com.mochao.module.ranking.client;

import com.mochao.module.ranking.dto.ScrapeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用 Node.js 抓取服务的 HTTP 客户端
 */
@Component
public class ScraperClient {

    private static final Logger log = LoggerFactory.getLogger(ScraperClient.class);

    private final RestTemplate restTemplate;
    private final String scraperBaseUrl;

    public ScraperClient(RestTemplate restTemplate,
                         @Value("${mochao.scraper.url:http://127.0.0.1:3001}") String scraperBaseUrl) {
        this.restTemplate = restTemplate;
        this.scraperBaseUrl = scraperBaseUrl;
    }

    /**
     * 健康检查
     */
    public boolean healthCheck() {
        try {
            Map<?, ?> result = restTemplate.getForObject(scraperBaseUrl + "/health", Map.class);
            return result != null && "ok".equals(result.get("status"));
        } catch (Exception e) {
            log.warn("抓取服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 触发抓取
     */
    public ScrapeResult scrape(String platform, String rankType) {
        Map<String, String> body = new HashMap<>();
        body.put("platform", platform);
        body.put("rankType", rankType);

        log.info("触发抓取: {}/{}", platform, rankType);
        return restTemplate.postForObject(scraperBaseUrl + "/scrape", body, ScrapeResult.class);
    }
}
