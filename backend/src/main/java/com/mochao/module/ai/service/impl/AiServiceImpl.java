package com.mochao.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.ai.dto.AiGenerateDTO;
import com.mochao.module.ai.dto.AiRequestDTO;
import com.mochao.module.ai.entity.AiConfig;
import com.mochao.module.ai.entity.AiPromptTemplate;
import com.mochao.module.ai.entity.AiUsageLog;
import com.mochao.module.ai.enums.AiFeatureType;
import com.mochao.module.ai.mapper.AiUsageLogMapper;
import com.mochao.module.ai.service.AiConfigService;
import com.mochao.module.ai.service.AiPromptTemplateService;
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

    /** AI 调用重试次数 */
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /** AI 调用重试间隔（毫秒） */
    private static final long RETRY_DELAY_MS = 1000;

    private final AiUsageLogMapper aiUsageLogMapper;
    private final NovelMapper novelMapper;
    private final NovelOutlineMapper novelOutlineMapper;
    private final NovelWorldviewMapper novelWorldviewMapper;
    private final NovelCharacterMapper novelCharacterMapper;
    private final NovelChapterMapper novelChapterMapper;
    private final NovelVolumeMapper novelVolumeMapper;
    private final NovelActMapper novelActMapper;
    private final AiConfigService aiConfigService;
    private final AiPromptTemplateService promptTemplateService;
    private final RestTemplate restTemplate;

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
                         NovelVolumeMapper novelVolumeMapper,
                         NovelActMapper novelActMapper,
                         AiConfigService aiConfigService,
                         AiPromptTemplateService promptTemplateService,
                         RestTemplate restTemplate) {
        this.aiUsageLogMapper = aiUsageLogMapper;
        this.novelMapper = novelMapper;
        this.novelOutlineMapper = novelOutlineMapper;
        this.novelWorldviewMapper = novelWorldviewMapper;
        this.novelCharacterMapper = novelCharacterMapper;
        this.novelChapterMapper = novelChapterMapper;
        this.novelVolumeMapper = novelVolumeMapper;
        this.novelActMapper = novelActMapper;
        this.aiConfigService = aiConfigService;
        this.promptTemplateService = promptTemplateService;
        this.restTemplate = restTemplate;
    }

    @Override
    public Map<String, Object> optimize(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位专业的文学编辑，擅长优化文字表达。请优化以下选中的文本，使其更加流畅、精准、有文学性。保持原意不变。";
        String userPrompt = buildUserPrompt(dto, "请优化以下文本：");
        String result = callAiWithRetry(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(),
                AiFeatureType.OPTIMIZE, dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> expand(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位网文创作大师，擅长细节描写和场景扩展。请对选中的文本进行扩写，增加更多细节、环境描写、心理活动等，使内容更丰满。";
        String userPrompt = buildUserPrompt(dto, "请扩写以下文本：");
        String result = callAiWithRetry(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(),
                AiFeatureType.EXPAND, dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> condense(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位文字精炼大师，擅长用最少的文字表达最核心的意思。请精简以下选中的文本，去掉冗余，保留精髓。";
        String userPrompt = buildUserPrompt(dto, "请精简以下文本：");
        String result = callAiWithRetry(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(),
                AiFeatureType.CONDENSE, dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> continueWriting(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位网文续写专家。请根据选中的文本和上下文，续写后续内容。保持文风一致，情节连贯。";
        String userPrompt = buildUserPrompt(dto, "请根据以下文本续写：");
        String result = callAiWithRetry(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(),
                AiFeatureType.CONTINUE, dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> polishDialogue(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位对话描写专家，擅长让角色对话更生动、更符合人物性格。请优化选中的对话文本，使其更加自然、有张力。";
        String userPrompt = buildUserPrompt(dto, "请优化以下对话：");
        String result = callAiWithRetry(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(),
                AiFeatureType.POLISH_DIALOGUE, dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> predict(AiRequestDTO dto, Long userId) {
        String systemPrompt = "你是一位网文情节策划专家。请根据选中的文本和上下文，预测/建议3个可能的情节发展方向。";
        String userPrompt = buildUserPrompt(dto, "请根据以下文本预测情节发展：");
        String result = callAiWithRetry(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), dto.getChapterId(),
                AiFeatureType.PREDICT, dto.getSelectedText(), result, buildContextSummary(dto.getNovelId()));
        return buildResult(result, logEntry.getId());
    }

    @Override
    public Map<String, Object> generate(AiGenerateDTO dto, Long userId) {
        AiFeatureType featureType = AiFeatureType.fromGenerateType(dto.getType());

        // 从模板获取 systemPrompt（用户自定义 > 系统默认 > 硬编码兜底）
        String systemPrompt = getSystemPrompt(userId, featureType.getCode());

        // 构建上下文（小说信息 + 各层级内容链式引用）
        String contextSummary = buildGenerateContext(dto, featureType);

        // 构建用户提示词
        String userPrompt = buildGenerateUserPrompt(dto, featureType, contextSummary);

        String result = callAiWithRetry(systemPrompt, userPrompt, userId);
        AiUsageLog logEntry = saveLog(userId, dto.getNovelId(), null,
                featureType, dto.getPrompt(), result, contextSummary);
        return buildResult(result, logEntry.getId());
    }

    /**
     * 获取提示词模板：用户自定义 > 系统默认 > 硬编码兜底
     */
    private String getSystemPrompt(Long userId, String featureCode) {
        AiPromptTemplate tpl = promptTemplateService.getTemplate(userId, featureCode);
        if (tpl != null && tpl.getSystemPrompt() != null && !tpl.getSystemPrompt().isEmpty()) {
            return tpl.getSystemPrompt();
        }
        // 硬编码兜底
        return getFallbackSystemPrompt(featureCode);
    }

    /**
     * 硬编码兜底提示词（当模板不存在时使用）
     */
    private String getFallbackSystemPrompt(String featureCode) {
        switch (featureCode) {
            case "generate-outline":
                return "你是一位网文大纲策划专家。请根据提供的信息直接生成小说大纲正文，使用 Markdown 格式。\n" +
                        "注意：不要添加任何开场白（如\"好的，根据您的要求\"）、结束语、总结性陈述或解释性语句。只输出大纲内容。";
            case "generate-volume-outline":
                return "你是一位网文卷纲策划专家。请根据提供的小说大纲直接生成卷纲正文，使用 Markdown 格式。\n" +
                        "注意：不要添加任何开场白、结束语、总结性陈述或解释性语句。只输出卷纲内容。";
            case "generate-act-outline":
                return "你是一位网文幕纲策划专家。请根据提供的小说大纲和卷纲直接生成幕纲正文，使用 Markdown 格式。\n" +
                        "注意：不要添加任何开场白、结束语、总结性陈述或解释性语句。只输出幕纲内容。";
            case "generate-detailed-outline":
                return "你是一位网文细纲策划专家。请根据提供的小说大纲、卷纲、幕纲直接生成章节细纲正文，使用 Markdown 格式。\n" +
                        "注意：不要添加任何开场白、结束语、总结性陈述或解释性语句。只输出细纲内容。";
            case "generate-character":
                return "你是一位人物设定专家。请根据提供的信息生成一个完整的人物设定。";
            case "generate-worldview":
                return "你是一位世界观架构专家。请根据提供的信息生成一个完整的世界观设定。";
            case "generate-chapter_outline":
                return "你是一位章节大纲专家。请根据提供的信息生成章节大纲。";
            default:
                return "你是一位专业的网文创作助手。请根据提供的信息生成内容。";
        }
    }

    /**
     * 构建生成类上下文（支持层级链式引用）
     *
     * 生成大纲时：小说简介 + 类型 + 书名
     * 生成卷纲时：大纲内容 + 用户输入
     * 生成幕纲时：卷纲内容 + 用户输入
     * 生成细纲时：幕纲内容 + 用户输入
     */
    private String buildGenerateContext(AiGenerateDTO dto, AiFeatureType featureType) {
        StringBuilder ctx = new StringBuilder();

        // 1. 基础信息（小说名+类型+简介）— 所有生成类型都需要
        Novel novel = novelMapper.selectById(dto.getNovelId());
        if (novel != null) {
            ctx.append("【小说基本信息】\n");
            ctx.append("书名：").append(novel.getTitle()).append("\n");
            if (novel.getGenre() != null) {
                ctx.append("类型：").append(novel.getGenre()).append("\n");
            }
            if (novel.getSummary() != null) {
                ctx.append("简介：").append(novel.getSummary()).append("\n");
            }
            ctx.append("\n");
        }

        // 2. 大纲内容（生成卷纲/幕纲/细纲/章纲时需要）
        if (needsOutlineContext(featureType)) {
            NovelOutline outline = novelOutlineMapper.selectOne(
                    new LambdaQueryWrapper<NovelOutline>().eq(NovelOutline::getNovelId, dto.getNovelId()));
            if (outline != null && outline.getContent() != null && !outline.getContent().isEmpty()) {
                ctx.append("【小说大纲】\n").append(outline.getContent()).append("\n\n");
            }
        }

        // 3. 父级内容（用户指定了 parentType + parentId）
        if (dto.getParentType() != null && dto.getParentId() != null) {
            appendParentContext(ctx, dto);
        }

        // 4. 卷纲上下文（生成幕纲和细纲时，如果没指定parent，加载所有卷纲作为参考）
        if (featureType == AiFeatureType.GENERATE_ACT_OUTLINE && dto.getParentId() == null) {
            appendAllVolumesContext(ctx, dto.getNovelId());
        }

        return ctx.toString();
    }

    /** 是否需要大纲上下文 */
    private boolean needsOutlineContext(AiFeatureType featureType) {
        return featureType == AiFeatureType.GENERATE_VOLUME_OUTLINE
                || featureType == AiFeatureType.GENERATE_ACT_OUTLINE
                || featureType == AiFeatureType.GENERATE_DETAILED_OUTLINE
                || featureType == AiFeatureType.GENERATE_CHAPTER_OUTLINE;
    }

    /** 附加父级上下文 */
    private void appendParentContext(StringBuilder ctx, AiGenerateDTO dto) {
        if ("volume".equals(dto.getParentType())) {
            NovelVolume volume = novelVolumeMapper.selectById(dto.getParentId());
            if (volume != null && volume.getOutline() != null && !volume.getOutline().isEmpty()) {
                ctx.append("【所属卷纲 — 第").append(volume.getVolumeNumber()).append("卷 ").append(volume.getTitle()).append("】\n");
                ctx.append(volume.getOutline()).append("\n\n");
            }
        } else if ("act".equals(dto.getParentType())) {
            NovelAct act = novelActMapper.selectById(dto.getParentId());
            if (act != null && act.getOutline() != null && !act.getOutline().isEmpty()) {
                ctx.append("【所属幕纲 — 第").append(act.getActNumber()).append("幕 ").append(act.getTitle()).append("】\n");
                ctx.append(act.getOutline()).append("\n\n");
                // 顺便加载所属卷纲
                NovelVolume volume = novelVolumeMapper.selectById(act.getVolumeId());
                if (volume != null && volume.getOutline() != null && !volume.getOutline().isEmpty()) {
                    ctx.append("【所属卷纲 — 第").append(volume.getVolumeNumber()).append("卷 ").append(volume.getTitle()).append("】\n");
                    ctx.append(volume.getOutline()).append("\n\n");
                }
            }
        }
    }

    /** 加载所有卷纲作为参考 */
    private void appendAllVolumesContext(StringBuilder ctx, Long novelId) {
        List<NovelVolume> volumes = novelVolumeMapper.selectList(
                new LambdaQueryWrapper<NovelVolume>()
                        .eq(NovelVolume::getNovelId, novelId)
                        .orderByAsc(NovelVolume::getSortOrder));
        if (!volumes.isEmpty()) {
            ctx.append("【全部卷纲概览】\n");
            for (NovelVolume v : volumes) {
                ctx.append("--- 第").append(v.getVolumeNumber()).append("卷 ").append(v.getTitle()).append(" ---\n");
                if (v.getOutline() != null) {
                    ctx.append(truncate(v.getOutline(), 800)).append("\n");
                }
            }
            ctx.append("\n");
        }
    }

    /** 构建用户提示词 */
    private String buildGenerateUserPrompt(AiGenerateDTO dto, AiFeatureType featureType, String context) {
        String instruction;
        switch (featureType) {
            case GENERATE_OUTLINE:
                instruction = "请根据以上小说的基本信息，生成一部完整的小说大纲（包含故事主线、分卷规划、核心冲突、人物关系网）。";
                break;
            case GENERATE_VOLUME_OUTLINE:
                instruction = "请根据以上小说大纲，为指定卷生成详细的卷纲（包含本卷主题、幕结构、情节节点、伏笔设计）。";
                break;
            case GENERATE_ACT_OUTLINE:
                instruction = "请根据以上大纲和卷纲，为指定幕生成详细的幕纲（包含核心冲突、场景序列、章节分布建议）。";
                break;
            case GENERATE_DETAILED_OUTLINE:
                instruction = "请根据以上大纲、卷纲、幕纲，生成详细的章节细纲（每章标注核心任务、情感目标、开头钩子、章节结尾钩子）。";
                break;
            case GENERATE_CHARACTER:
                instruction = "请根据以上信息，生成一个完整的人物设定（包括外貌、性格、背景、关系等）。";
                break;
            case GENERATE_WORLDVIEW:
                instruction = "请根据以上信息，生成一个完整的世界观设定。";
                break;
            case GENERATE_CHAPTER_OUTLINE:
                instruction = "请根据以上信息，生成章节大纲。";
                break;
            default:
                instruction = "请根据以上信息生成内容。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("上下文：\n").append(context).append("\n");
        sb.append("用户要求：").append(dto.getPrompt()).append("\n\n");
        sb.append(instruction);
        return sb.toString();
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
            factory.setReadTimeout(300000);
        } else {
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(300000);
        }
        return new RestTemplate(factory);
    }

    private boolean isCustomProxy(AiRuntimeConfig cfg) {
        String cfgHost = cfg.proxyHost != null ? cfg.proxyHost : "";
        int cfgPort = cfg.proxyPort != null ? cfg.proxyPort : 0;
        String defHost = defaultProxyHost != null ? defaultProxyHost : "";
        int defPort = defaultProxyPort != null ? defaultProxyPort : 0;
        return !cfgHost.equals(defHost) || cfgPort != defPort;
    }

    // ==================== AI 调用（含重试机制） ====================

    /**
     * 调用 AI API — 带自动重试（最多 3 次，间隔 1 秒递增）
     *
     * 旧代码：单次调用失败直接抛 "AI服务调用失败"
     * 新代码：网络抖动/超时自动重试，仅最终失败才抛异常
     */
    private String callAiWithRetry(String systemPrompt, String userPrompt, Long userId) {
        AiRuntimeConfig cfg = getRuntimeConfig(userId);
        RestTemplate rt = isCustomProxy(cfg) ? buildRestTemplate(cfg.proxyHost, cfg.proxyPort) : this.restTemplate;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return callAiOnce(rt, cfg, systemPrompt, userPrompt);
            } catch (BusinessException e) {
                // 业务异常（如"AI返回内容为空"）不重试，直接抛
                throw e;
            } catch (Exception e) {
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    long delay = RETRY_DELAY_MS * attempt; // 递增延迟: 1s, 2s
                    log.warn("AI调用失败（第{}次），{}ms后重试: {}", attempt, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI调用被中断");
                    }
                } else {
                    log.error("AI调用最终失败（已重试{}次）: ", MAX_RETRY_ATTEMPTS, e);
                    throw new BusinessException(ResultCode.INTERNAL_ERROR,
                            "AI服务调用失败（已重试" + MAX_RETRY_ATTEMPTS + "次）: " + e.getMessage());
                }
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI服务调用失败");
    }

    /**
     * 单次 AI API 调用（不含重试逻辑）
     */
    private String callAiOnce(RestTemplate rt, AiRuntimeConfig cfg,
                              String systemPrompt, String userPrompt) {
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
                String content = (String) message.get("content");

                // 🔧 修复 Token 统计：从 API 响应中提取真实 usage 数据
                int realTokensUsed = extractTokenUsage(response);
                saveTokenUsageToThreadLocal(realTokensUsed);

                return content;
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI返回内容为空");
    }

    /**
     * 🔧 从 OpenAI 兼容 API 响应中提取真实 Token 使用量
     *
     * 旧代码: outputText.length() — 完全不准（1个中文字符 ≠ 1个token）
     * 新代码: 从 response.usage.total_tokens 提取真实值
     *         如果 API 不返回 usage，则用估算公式近似
     */
    private int extractTokenUsage(Map<String, Object> response) {
        // 尝试从 API 响应的 usage 字段提取
        if (response.containsKey("usage")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            Object totalTokens = usage.get("total_tokens");
            if (totalTokens instanceof Number) {
                return ((Number) totalTokens).intValue();
            }
        }
        // Fallback: OpenAI 兼容 API 不返回 usage 时，用估算公式
        // 英文 ~4 chars/token，中文 ~2 chars/token，混合取 ~3 chars/token
        return 0; // 无法估算时返回 0，由 saveLog 处理
    }

    // ThreadLocal 传递真实 token 用量到 saveLog
    private static final ThreadLocal<Integer> tokenUsageHolder = new ThreadLocal<>();

    private void saveTokenUsageToThreadLocal(int tokens) {
        tokenUsageHolder.set(tokens);
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

    /**
     * 🔧 saveLog 改用枚举 + 真实 Token 统计
     *
     * 旧代码: feature 用魔法字符串，tokensUsed 用 outputText.length()
     * 新代码: feature 用枚举 getCode()，tokensUsed 从 ThreadLocal 取真实值
     */
    private AiUsageLog saveLog(Long userId, Long novelId, Long chapterId,
                                AiFeatureType feature,
                                String inputText, String outputText, String contextSummary) {
        AiUsageLog logEntry = new AiUsageLog();
        logEntry.setUserId(userId);
        logEntry.setNovelId(novelId);
        logEntry.setChapterId(chapterId);
        logEntry.setFeature(feature.getCode()); // 枚举替代魔法字符串
        logEntry.setInputText(inputText);
        logEntry.setOutputText(outputText);
        logEntry.setContextSummary(contextSummary);

        // 🔧 Token 统计：优先用 API 返回的真实值，fallback 用估算公式
        Integer realTokens = tokenUsageHolder.get();
        if (realTokens != null && realTokens > 0) {
            logEntry.setTokensUsed(realTokens);
        } else {
            // 估算 fallback：中文 ~2 chars/token，英文 ~4 chars/token，混合取 ~3 chars/token
            int estimated = (outputText != null ? outputText.length() / 3 : 0)
                    + (inputText != null ? inputText.length() / 3 : 0);
            logEntry.setTokensUsed(Math.max(1, estimated));
        }
        tokenUsageHolder.remove(); // 清理 ThreadLocal

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
