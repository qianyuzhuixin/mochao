package com.mochao.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.ai.dto.AiConfigDTO;
import com.mochao.module.ai.entity.AiConfig;
import com.mochao.module.ai.mapper.AiConfigMapper;
import com.mochao.module.ai.service.AiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AiConfigServiceImpl implements AiConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiConfigServiceImpl.class);

    private final AiConfigMapper aiConfigMapper;

    public AiConfigServiceImpl(AiConfigMapper aiConfigMapper) {
        this.aiConfigMapper = aiConfigMapper;
    }

    @Override
    public List<AiConfig> listByUser(Long userId) {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getUserId, userId)
                .orderByDesc(AiConfig::getIsActive)
                .orderByDesc(AiConfig::getUpdatedAt);
        return aiConfigMapper.selectList(wrapper);
    }

    @Override
    public AiConfig getById(Long id, Long userId) {
        AiConfig config = aiConfigMapper.selectById(id);
        if (config == null || !config.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置不存在");
        }
        return config;
    }

    @Override
    public AiConfig create(AiConfigDTO dto, Long userId) {
        AiConfig config = new AiConfig();
        config.setUserId(userId);
        config.setProviderName(dto.getProviderName());
        config.setApiUrl(dto.getApiUrl());
        config.setApiKey(dto.getApiKey());
        config.setModel(dto.getModel());
        config.setMaxTokens(dto.getMaxTokens() != null ? dto.getMaxTokens() : 2000);
        config.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : 0.8);
        config.setProxyHost(dto.getProxyHost());
        config.setProxyPort(dto.getProxyPort());
        config.setIsActive(false);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        aiConfigMapper.insert(config);

        // 如果是第一个配置，自动激活
        long count = aiConfigMapper.selectCount(
                new LambdaQueryWrapper<AiConfig>().eq(AiConfig::getUserId, userId));
        if (count == 1) {
            activateInDb(config.getId());
        }

        return config;
    }

    @Override
    public AiConfig update(AiConfigDTO dto, Long userId) {
        AiConfig config = getById(dto.getId(), userId);
        config.setProviderName(dto.getProviderName());
        config.setApiUrl(dto.getApiUrl());
        config.setApiKey(dto.getApiKey());
        config.setModel(dto.getModel());
        config.setMaxTokens(dto.getMaxTokens() != null ? dto.getMaxTokens() : 2000);
        config.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : 0.8);
        config.setProxyHost(dto.getProxyHost());
        config.setProxyPort(dto.getProxyPort());
        config.setUpdatedAt(LocalDateTime.now());
        aiConfigMapper.updateById(config);
        return config;
    }

    @Override
    public void delete(Long id, Long userId) {
        AiConfig config = getById(id, userId);
        if (Boolean.TRUE.equals(config.getIsActive())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能删除正在使用的配置，请先切换到其他配置");
        }
        aiConfigMapper.deleteById(id);
    }

    @Override
    public void activate(Long id, Long userId) {
        AiConfig config = getById(id, userId);

        // 取消当前用户所有激活状态
        AiConfig update = new AiConfig();
        update.setIsActive(false);
        aiConfigMapper.update(update,
                new LambdaQueryWrapper<AiConfig>().eq(AiConfig::getUserId, userId));

        // 激活目标
        activateInDb(id);
    }

    @Override
    public Map<String, Object> testConnection(AiConfigDTO dto) {
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();

        try {
            RestTemplate rt = buildRestTemplate(dto.getProxyHost(), dto.getProxyPort());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(dto.getApiKey());

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "Hi");
            messages.add(userMsg);

            Map<String, Object> body = new HashMap<>();
            body.put("model", dto.getModel());
            body.put("messages", messages);
            body.put("max_tokens", 10);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = rt.postForObject(dto.getApiUrl(), entity, Map.class);

            long elapsed = System.currentTimeMillis() - start;
            result.put("success", response != null && response.containsKey("choices"));
            result.put("latency", elapsed);
            result.put("message", response != null ? "连接成功" : "无响应");
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            result.put("success", false);
            result.put("latency", elapsed);
            result.put("message", extractErrorMessage(e));
        }
        return result;
    }

    @Override
    public AiConfig getActiveConfig(Long userId) {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getUserId, userId)
                .eq(AiConfig::getIsActive, true);
        return aiConfigMapper.selectOne(wrapper);
    }

    // ==================== Private ====================

    private void activateInDb(Long id) {
        AiConfig update = new AiConfig();
        update.setId(id);
        update.setIsActive(true);
        update.setUpdatedAt(LocalDateTime.now());
        aiConfigMapper.updateById(update);
    }

    private RestTemplate buildRestTemplate(String proxyHost, Integer proxyPort) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && proxyPort > 0) {
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        }
        return new RestTemplate(factory);
    }

    private String extractErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "未知错误";
        if (msg.contains("Connection timed out")) return "连接超时，请检查网络或配置代理";
        if (msg.contains("401")) return "API Key 无效（401 Unauthorized）";
        if (msg.contains("403")) return "访问被拒绝（403 Forbidden），请检查 API Key 权限";
        if (msg.contains("404")) return "API 地址不存在（404 Not Found）";
        if (msg.contains("429")) return "请求过于频繁（429），请稍后重试";
        if (msg.contains("500")) return "服务器内部错误（500）";
        if (msg.length() > 100) return msg.substring(0, 100) + "...";
        return msg;
    }
}
