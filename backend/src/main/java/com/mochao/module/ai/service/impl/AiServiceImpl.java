package com.mochao.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.ai.dto.AiGenerateDTO;
import com.mochao.module.ai.dto.AiRequestDTO;
import com.mochao.module.ai.entity.AiConfig;
import com.mochao.module.ai.entity.AiUsageLog;
import com.mochao.module.ai.mapper.AiUsageLogMapper;
import com.mochao.module.ai.service.AiConfigService;
import com.mochao.module.ai.service.AiService;
import com.mochao.module.novel.entity.*;
import com.mochao.module.novel.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final AiUsageLogMapper aiUsageLogMapper;
    private final NovelMapper novelMapper;
    private final NovelOutlineMapper novelOutlineMapper;
    private final NovelWorldviewMapper novelWorldviewMapper;
    private final NovelCharacterMapper novelCharacterMapper;
    private final NovelChapterMapper novelChapterMapper;
    private final AiConfigService aiConfigService;

    /** application.yml 兜底配置 */
    @Value("${ai.api-url}")
    private String defaultApiUrl;

    @Value("${ai.api-key}")
    private String defaultApiKey;

    @Value("${ai.model}")
    private String defaultModel;

    @Value("${ai.max-tokens}")
    private int defaultMaxTokens;

    @Value("${ai.temperature}")
    private double defaultTemperature;

    @Value("${ai.proxy-host:}")
    private String defaultProxyHost;

    @Value("${ai.proxy-port:0}")
    private Integer defaultProxyPort;

    public AiServiceImpl(AiUsageLogMapper aiUsageLogMapper,
                         NovelMapper novelMapper,
                         NovelOutlineMapper novelOutlineMapper,
                         NovelWorldviewMapper novelWorldviewMapper,
                         NovelCharacterMapper novelCharacterMapper,
                         NovelChapterMapper novelChapterMapper,
                         AiConfigService aiConfigService) {
        this.aiUsageLogMapper = aiUsageLogMapper;
        this.novelMapper = novelMapper;
        this.novelOutlineMapper = novelOutlineMapper;
        this.novelWorldviewMapper = novelWorldviewMapper;
        this.novelCharacterMapper = novelCharacterMapper;
        this.novelChapterMapper = novelChapterMapper;
        this.aiConfigService = aiConfigService;
    }

    @Override
    public Map<String, Object> optimize(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位专业的文学编辑，擅长优化文字表达。请优化以下选中的文本，使其更加流畅、精准、有文学性。保持原意不变。";
        String userPrompt = buildUserPrompt(dto, "请优化以下文本：");
        String result = callAi(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(), "optimize",
                dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> expand(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位网文创作大师，擅长细节描写和场景扩展。请对选中的文本进行扩写，增加更多细节、环境描写、心理活动等，使内容更丰满。";
        String userPrompt = buildUserPrompt(dto, "请扩写以下文本：");
        String result = callAi(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(), "expand",
                dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> condense(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位文字精炼大师，擅长用最少的文字表达最核心的意思。请精简以下选中的文本，去掉冗余，保留精髓。";
        String userPrompt = buildUserPrompt(dto, "请精简以下文本：");
        String result = callAi(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(), "condense",
                dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> continueWriting(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位网文续写专家。请根据选中的文本和上下文，续写后续内容。保持文风一致，情节连贯。";
        String userPrompt = buildUserPrompt(dto, "请根据以下文本续写：");
        String result = callAi(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(), "continue",
                dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> polishDialogue(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位对话描写专家，擅长让角色对话更生动、更符合人物性格。请优化选中的对话文本，使其更加自然、有张力。";
        String userPrompt = buildUserPrompt(dto, "请优化以下对话：");
        String result = callAi(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(), "polish-dialogue",
                dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> predict(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位网文情节策划专家。请根据选中的文本和上下文，预测/建议3个可能的情节发展方向。";
        String userPrompt = buildUserPrompt(dto, "请根据以下文本预测情节发展：");
        String result = callAi(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(), "predict",
                dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> generate(AiGenerateDTO dto, Long userId) {
        String systemPrompt;
        String userPrompt;
        String contextSummary = buildContextSummary(dto.getNovelId());

        switch (dto.getType()) {
            case "outline":
                systemPrompt = "你是一位网文大纲策划专家。请根据提供的信息生成小说大纲。";
                userPrompt = "上下文：\n" + contextSummary + "\n\n用户要求：" + dto.getPrompt() + "\n\n请生成小说大纲：";
                break;
            case "character":
                systemPrompt = "你是一位人物设定专家。请根据提供的信息生成一个完整的人物设定，包括外貌、性格、背景、关系等。";
                userPrompt = "上下文：\n" + contextSummary + "\n\n用户要求：" + dto.getPrompt() + "\n\n请生成人物设定：";
                break;
            case "worldview":
                systemPrompt = "你是一位世界观架构专家。请根据提供的信息生成一个完整的世界观设定。";
                userPrompt = "上下文：\n" + contextSummary + "\n\n用户要求：" + dto.getPrompt() + "\n\n请生成世界观设定：";
                break;
            case "chapter_outline":
                systemPrompt = "你是一位章节大纲专家。请根据提供的信息生成章节大纲。";
                userPrompt = "上下文：\n" + contextSummary + "\n\n用户要求：" + dto.getPrompt() + "\n\n请生成章节大纲：";
                break;
            default:
                throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的生成类型: " + dto.getType());
        }

        String result = callAi(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), null, "generate-" + dto.getType(),
                dto.getPrompt(), result, contextSummary);
        return buildResult(result, logEntry.getId());
    }

    @Override
    public void adopt(Long logId, Long userId) {
        AiUsageLog aiLog = aiUsageLogMapper.selectById(logId);
        if (aiLog == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI记录不存在");
        }
        if (!userId.equals(aiLog.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作");
        }
        aiLog.setAdopted(true);
        aiUsageLogMapper.updateById(aiLog);
    }

    @Override
    public Page<AiUsageLog> getHistory(Long userId, Integer page, Integer size) {
        Page<AiUsageLog> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<AiUsageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiUsageLog::getUserId, userId)
                .orderByDesc(AiUsageLog::getCreatedAt);
        return aiUsageLogMapper.selectPage(pageObj, wrapper);
    }

    // ==================== Dynamic Config ====================

    /**
     * 获取当前用户的 AI 配置：优先 DB 激活配置，否则 fallback 到 application.yml
     */
    private AiRuntimeConfig getRuntimeConfig(Long userId) {
        AiConfig dbConfig = aiConfigService.getActiveConfig(userId);
        if (dbConfig != null) {
            return new AiRuntimeConfig(
                    dbConfig.getApiUrl(),
                    dbConfig.getApiKey(),
                    dbConfig.getModel(),
                    dbConfig.getMaxTokens() != null ? dbConfig.getMaxTokens() : defaultMaxTokens,
                    dbConfig.getTemperature() != null ? dbConfig.getTemperature() : defaultTemperature,
                    dbConfig.getProxyHost(),
                    dbConfig.getProxyPort()
            );
        }
        return new AiRuntimeConfig(
                defaultApiUrl,
                defaultApiKey,
                defaultModel,
                defaultMaxTokens,
                defaultTemperature,
                defaultProxyHost,
                defaultProxyPort
        );
    }

    private RestTemplate buildRestTemplate(String proxyHost, Integer proxyPort) {
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

    // ==================== Private Methods ====================

    private String buildUserPrompt(AiRequestDTO dto, String instruction) {
        StringBuilder sb = new StringBuilder();
        String context = buildContextSummary(dto.getNovelId());
        if (context != null && !context.isEmpty()) {
            sb.append("上下文：\n").append(context).append("\n\n");
        }
        if (dto.getChapterId() != null) {
            NovelChapter chapter = novelChapterMapper.selectById(dto.getChapterId());
            if (chapter != null && chapter.getContent() != null) {
                String content = chapter.getContent();
                String surrounding = extractSurrounding(content, dto.getSelectedText());
                if (surrounding != null) {
                    sb.append("前文内容：\n").append(surrounding).append("\n\n");
                }
            }
        }
        sb.append(instruction).append("\n\n").append(dto.getSelectedText());
        if (dto.getCustomPrompt() != null && !dto.getCustomPrompt().isEmpty()) {
            sb.append("\n\n额外要求：").append(dto.getCustomPrompt());
        }
        return sb.toString();
    }

    private String buildContextSummary(Long novelId) {
        if (novelId == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Novel novel = novelMapper.selectById(novelId);
        if (novel != null) {
            sb.append("小说标题：").append(novel.getTitle()).append("\n");
            if (novel.getSummary() != null) {
                sb.append("简介：").append(novel.getSummary()).append("\n");
            }
        }

        NovelOutline outline = novelOutlineMapper.selectOne(
                new LambdaQueryWrapper<NovelOutline>().eq(NovelOutline::getNovelId, novelId));
        if (outline != null && outline.getContent() != null) {
            sb.append("大纲摘要：").append(truncate(outline.getContent(), 500)).append("\n");
        }

        NovelWorldview worldview = novelWorldviewMapper.selectOne(
                new LambdaQueryWrapper<NovelWorldview>().eq(NovelWorldview::getNovelId, novelId));
        if (worldview != null && worldview.getContent() != null) {
            sb.append("世界观：").append(truncate(worldview.getContent(), 500)).append("\n");
        }

        List<NovelCharacter> characters = novelCharacterMapper.selectList(
                new LambdaQueryWrapper<NovelCharacter>().eq(NovelCharacter::getNovelId, novelId));
        if (!characters.isEmpty()) {
            sb.append("主要人物：\n");
            for (NovelCharacter c : characters) {
                sb.append("- ").append(c.getName());
                if (c.getRole() != null) sb.append("（").append(c.getRole()).append("）");
                if (c.getPersonality() != null) sb.append("：").append(truncate(c.getPersonality(), 100));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String extractSurrounding(String content, String selectedText) {
        int idx = content.indexOf(selectedText);
        if (idx < 0) {
            return null;
        }
        int start = Math.max(0, idx - 500);
        int end = Math.min(content.length(), idx + selectedText.length() + 500);
        return content.substring(start, end);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String callAi(String systemPrompt, String userPrompt, Long userId) {
        AiRuntimeConfig cfg = getRuntimeConfig(userId);
        RestTemplate rt = buildRestTemplate(cfg.proxyHost, cfg.proxyPort);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cfg.apiKey);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", cfg.model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", cfg.maxTokens);
            requestBody.put("temperature", cfg.temperature);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = rt.postForObject(cfg.apiUrl, entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI返回内容为空");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI调用失败: ", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI服务调用失败: " + e.getMessage());
        }
    }

    private AiUsageLog saveLog(Long userId, Long novelId, Long chapterId, String feature,
                                String inputText, String outputText, String contextSummary) {
        AiUsageLog logEntry = new AiUsageLog();
        logEntry.setUserId(userId);
        logEntry.setNovelId(novelId);
        logEntry.setChapterId(chapterId);
        logEntry.setFeature(feature);
        logEntry.setInputText(inputText);
        logEntry.setOutputText(outputText);
        logEntry.setContextSummary(contextSummary);
        logEntry.setTokensUsed(outputText != null ? outputText.length() : 0);
        logEntry.setAdopted(false);
        logEntry.setCreatedAt(LocalDateTime.now());
        aiUsageLogMapper.insert(logEntry);
        return logEntry;
    }

    private Map<String, Object> buildResult(String content, Long logId) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("logId", logId);
        return result;
    }

    // ==================== Inner class ====================

    private static class AiRuntimeConfig {
        final String apiUrl;
        final String apiKey;
        final String model;
        final int maxTokens;
        final double temperature;
        final String proxyHost;
        final Integer proxyPort;

        AiRuntimeConfig(String apiUrl, String apiKey, String model, int maxTokens,
                        double temperature, String proxyHost, Integer proxyPort) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.maxTokens = maxTokens;
            this.temperature = temperature;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
        }
    }
}
