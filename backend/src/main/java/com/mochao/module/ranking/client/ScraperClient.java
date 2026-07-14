package com.mochao.module.ranking.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochao.module.ranking.dto.ScrapeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用 Node.js 抓取服务的 HTTP 客户端
 */
@Component
public class ScraperClient {

    private static final Logger log = LoggerFactory.getLogger(ScraperClient.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final RestTemplate restTemplate;
    private final RestTemplate downloadRestTemplate;  // 整本下载专用（5 分钟超时）
    private final String scraperBaseUrl;

    public ScraperClient(RestTemplate restTemplate,
                         @Value("${mochao.scraper.url:http://127.0.0.1:3001}") String scraperBaseUrl) {
        this.restTemplate = restTemplate;
        this.scraperBaseUrl = scraperBaseUrl;

        // 整本下载可能耗时较长，创建专用 RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(300000);  // 5 分钟
        this.downloadRestTemplate = new RestTemplate(factory);
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

    /**
     * 搜索小说
     * @param platform 平台 (目前仅支持 fanqie)
     * @param keyword 搜索关键词（书名/作者）
     * @param page 页码 (0 起始)
     * @param pageSize 每页数量
     * @return 搜索结果 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> searchBooks(String platform, String keyword, int page, int pageSize) {
        Map<String, Object> body = new HashMap<>();
        body.put("platform", platform);
        body.put("keyword", keyword);
        body.put("page", page);
        body.put("pageSize", pageSize);

        log.info("搜索小说: {}/{} keyword={}, page={}", platform, keyword, page);

        try {
            String response = restTemplate.postForObject(
                    scraperBaseUrl + "/search",
                    body,
                    String.class
            );
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            log.error("搜索小说失败: {}/{} - {}", platform, keyword, e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "搜索服务调用失败");
            errorResult.put("detail", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 下载整本小说
     * @param platform 平台 (目前仅支持 fanqie)
     * @param bookId 书籍 ID
     * @param maxChapters 最多下载章节数 (0 = 全部)
     * @return 下载结果 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> downloadBook(String platform, String bookId, int maxChapters) {
        Map<String, Object> body = new HashMap<>();
        body.put("platform", platform);
        body.put("bookId", bookId);
        body.put("maxChapters", maxChapters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("下载小说: {}/{} (maxChapters={})", platform, bookId, maxChapters);

        try {
            // 整本下载使用专用 RestTemplate（5 分钟超时）
            String response = downloadRestTemplate.postForObject(
                    scraperBaseUrl + "/download",
                    new HttpEntity<>(body, headers),
                    String.class
            );
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            log.error("下载小说失败: {}/{} - {}", platform, bookId, e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "下载服务调用失败");
            errorResult.put("detail", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 下载文件（txt/html/pdf），返回原始字节流
     * @param platform 平台
     * @param bookId 书籍 ID
     * @param format 输出格式 (txt|html|pdf)
     * @param maxChapters 最多下载章节数 (0 = 全部)
     * @return ResponseEntity 包含文件字节、Content-Type 和 Content-Disposition
     */
    public ResponseEntity<byte[]> downloadFile(String platform, String bookId, String format, int maxChapters) {
        Map<String, Object> body = new HashMap<>();
        body.put("platform", platform);
        body.put("bookId", bookId);
        body.put("format", format);
        body.put("maxChapters", maxChapters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("下载文件: {}/{} format={} maxChapters={}", platform, bookId, format, maxChapters);

        return downloadRestTemplate.postForEntity(
                scraperBaseUrl + "/download-file",
                new HttpEntity<>(body, headers),
                byte[].class
        );
    }
}
