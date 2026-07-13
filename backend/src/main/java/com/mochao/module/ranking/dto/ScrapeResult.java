package com.mochao.module.ranking.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 抓取结果（从 Node.js 服务返回）
 */
@Data
public class ScrapeResult {

    private boolean success;
    private String platform;
    private String rankType;
    private int count;
    private List<Map<String, Object>> items;
    private String scrapedAt;

    /** Node.js 服务返回的 error 字段 */
    private String error;

    /** 自定义提示消息（如"今天已有数据"） */
    private String message;
}
