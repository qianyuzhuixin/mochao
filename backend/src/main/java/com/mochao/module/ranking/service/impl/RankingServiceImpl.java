package com.mochao.module.ranking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mochao.module.ranking.client.ScraperClient;
import com.mochao.module.ranking.dto.RankingQueryDTO;
import com.mochao.module.ranking.dto.ScrapeResult;
import com.mochao.module.ranking.entity.RankingSnapshot;
import com.mochao.module.ranking.mapper.RankingSnapshotMapper;
import com.mochao.module.ranking.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RankingServiceImpl implements RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingServiceImpl.class);

    /** Redis key 前缀：标记当天是否已有数据 */
    private static final String REDIS_FLAG_KEY = "ranking:flag:%s:%s:%s";
    /** Redis key 前缀：榜单全量数据缓存 */
    private static final String REDIS_LIST_KEY = "ranking:list:%s:%s:%s";
    /** Redis TTL */
    private static final long REDIS_TTL_HOURS = 24;

    /** 共享 ObjectMapper（支持 Java8 时间） */
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final ScraperClient scraperClient;
    private final RankingSnapshotMapper rankingSnapshotMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public RankingServiceImpl(ScraperClient scraperClient,
                              RankingSnapshotMapper rankingSnapshotMapper,
                              StringRedisTemplate stringRedisTemplate) {
        this.scraperClient = scraperClient;
        this.rankingSnapshotMapper = rankingSnapshotMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // ==================== 抓取 ====================

    @Override
    @Transactional
    public ScrapeResult triggerScrape(String platform, String rankType) {
        LocalDate today = LocalDate.now();

        // 0. 今天已有数据 → 跳过
        if (checkTodayData(platform, rankType)) {
            log.info("当天已有数据，跳过抓取: {} {}", platform, rankType);
            ScrapeResult exists = new ScrapeResult();
            exists.setSuccess(true);
            exists.setPlatform(platform);
            exists.setRankType(rankType);
            exists.setMessage("今天已有数据，无需重复抓取");
            return exists;
        }

        // 1. 健康检查
        if (!scraperClient.healthCheck()) {
            ScrapeResult fail = new ScrapeResult();
            fail.setSuccess(false);
            fail.setError("抓取服务不可用，请确认 scraper 服务已启动（localhost:3001）");
            return fail;
        }

        // 2. 调用 Node.js 抓取服务
        ScrapeResult result = scraperClient.scrape(platform, rankType);
        if (!result.isSuccess() || result.getItems() == null || result.getItems().isEmpty()) {
            return result;
        }

        // 3. 删除当天旧快照（幂等覆盖）
        rankingSnapshotMapper.delete(
                new LambdaQueryWrapper<RankingSnapshot>()
                        .eq(RankingSnapshot::getPlatform, platform)
                        .eq(RankingSnapshot::getRankType, rankType)
                        .eq(RankingSnapshot::getSnapDate, today)
        );

        // 4. 保存新快照到数据库
        List<RankingSnapshot> snapshots = result.getItems().stream().map(item -> {
            RankingSnapshot s = new RankingSnapshot();
            s.setPlatform(platform);
            s.setRankType(rankType);
            s.setRankNo(toInt(item.get("rankNo")));
            s.setBookName(toString(item.get("bookName")));
            s.setAuthor(toString(item.get("author")));
            s.setCategory(toString(item.get("category")));
            s.setWordCount(toLong(item.get("wordCount")));
            s.setHotValue(toLong(item.get("hotValue")));
            s.setIntro(toString(item.get("intro")));
            s.setCoverUrl(toString(item.get("coverUrl")));
            s.setBookUrl(toString(item.get("bookUrl")));
            s.setSnapDate(today);
            return s;
        }).collect(Collectors.toList());

        for (RankingSnapshot snapshot : snapshots) {
            rankingSnapshotMapper.insert(snapshot);
        }

        // 5. 缓存到 Redis
        cacheToRedis(platform, rankType, today, snapshots);

        log.info("榜单快照已保存并缓存: {} {} -- {} 条", platform, rankType, snapshots.size());
        return result;
    }

    // ==================== 查询 ====================

    @Override
    public Page<RankingSnapshot> querySnapshots(RankingQueryDTO dto) {
        LocalDate queryDate = dto.getSnapDate() != null && !dto.getSnapDate().isEmpty()
                ? LocalDate.parse(dto.getSnapDate())
                : LocalDate.now();

        String platform = dto.getPlatform();
        String rankType = dto.getRankType();

        // 1. 优先从 Redis 缓存加载全量数据
        List<RankingSnapshot> allSnapshots = getFromRedisCache(platform, rankType, queryDate);

        // 2. 缓存未命中 → 查询 DB
        if (allSnapshots == null) {
            allSnapshots = queryFromDB(platform, rankType, queryDate);

            // 今天的数据缓存到 Redis
            if (queryDate.equals(LocalDate.now()) && !allSnapshots.isEmpty()) {
                cacheToRedis(platform, rankType, queryDate, allSnapshots);
            }
        }

        // 3. 内存分页
        return paginateInMemory(allSnapshots, dto.getPage(), dto.getSize());
    }

    // ==================== 日期标记 ====================

    @Override
    public boolean checkTodayData(String platform, String rankType) {
        LocalDate today = LocalDate.now();
        String flagKey = String.format(REDIS_FLAG_KEY, platform, rankType, today);

        // 先查 Redis 标记
        String flag = stringRedisTemplate.opsForValue().get(flagKey);
        if ("1".equals(flag)) {
            return true;
        }

        // Redis 没有 → 查 DB
        Long count = rankingSnapshotMapper.selectCount(
                new LambdaQueryWrapper<RankingSnapshot>()
                        .eq(RankingSnapshot::getPlatform, platform)
                        .eq(RankingSnapshot::getRankType, rankType)
                        .eq(RankingSnapshot::getSnapDate, today)
        );
        boolean exists = count != null && count > 0;
        if (exists) {
            // 补写 Redis 标记
            stringRedisTemplate.opsForValue().set(flagKey, "1", REDIS_TTL_HOURS, TimeUnit.HOURS);
        }
        return exists;
    }

    @Override
    public List<String> getAvailableDates(String platform, String rankType) {
        // 查询所有不重复的快照日期
        List<RankingSnapshot> snapshots = rankingSnapshotMapper.selectList(
                new LambdaQueryWrapper<RankingSnapshot>()
                        .select(RankingSnapshot::getSnapDate)
                        .eq(RankingSnapshot::getPlatform, platform)
                        .eq(RankingSnapshot::getRankType, rankType)
                        .groupBy(RankingSnapshot::getSnapDate)
                        .orderByDesc(RankingSnapshot::getSnapDate)
        );
        return snapshots.stream()
                .map(s -> s.getSnapDate().toString())
                .collect(Collectors.toList());
    }

    // ==================== 搜索 ====================

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchBooks(String keyword, String platform, int limit) {
        // 调用 scraper 的番茄小说实时搜索 API（仿 fanqienovel-downloader）
        String searchPlatform = (platform != null && !platform.isEmpty()) ? platform : "fanqie";
        Map<String, Object> scraperResult = scraperClient.searchBooks(searchPlatform, keyword, 0, limit);
        if (scraperResult == null || !Boolean.TRUE.equals(scraperResult.get("success"))) {
            log.warn("番茄搜索API调用失败: {}", scraperResult != null ? scraperResult.get("error") : "null");
            return Collections.emptyList();
        }

        List<Map<String, Object>> books = (List<Map<String, Object>>) scraperResult.get("books");
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }

        // 统一格式：为每个结果添加 platform 字段
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> b : books) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bookId", b.getOrDefault("bookId", ""));
            item.put("bookName", b.getOrDefault("bookName", ""));
            item.put("author", b.getOrDefault("author", ""));
            item.put("platform", searchPlatform);
            item.put("category", b.getOrDefault("category", ""));
            item.put("wordCount", b.getOrDefault("wordCount", 0));
            item.put("coverUrl", b.getOrDefault("coverUrl", ""));
            item.put("bookUrl", b.getOrDefault("bookUrl", ""));
            item.put("intro", b.getOrDefault("abstract", ""));
            item.put("status", b.getOrDefault("status", ""));
            results.add(item);
            if (results.size() >= limit) break;
        }
        return results;
    }

    // ==================== Redis 缓存 ====================

    /**
     * 将榜单全量数据 + 标记写入 Redis
     */
    private void cacheToRedis(String platform, String rankType, LocalDate date, List<RankingSnapshot> snapshots) {
        try {
            String flagKey = String.format(REDIS_FLAG_KEY, platform, rankType, date);
            String listKey = String.format(REDIS_LIST_KEY, platform, rankType, date);
            String json = objectMapper.writeValueAsString(snapshots);

            stringRedisTemplate.opsForValue().set(flagKey, "1", REDIS_TTL_HOURS, TimeUnit.HOURS);
            stringRedisTemplate.opsForValue().set(listKey, json, REDIS_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败，不影响主流程: {}", e.getMessage());
        }
    }

    /**
     * 从 Redis 加载缓存数据（未命中返回 null）
     */
    private List<RankingSnapshot> getFromRedisCache(String platform, String rankType, LocalDate date) {
        try {
            String listKey = String.format(REDIS_LIST_KEY, platform, rankType, date);
            String json = stringRedisTemplate.opsForValue().get(listKey);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<RankingSnapshot>>() {});
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败，回退到 DB: {}", e.getMessage());
            return null;
        }
    }

    // ==================== DB 查询 ====================

    /**
     * 直接从数据库查询全量（不分页）
     */
    private List<RankingSnapshot> queryFromDB(String platform, String rankType, LocalDate date) {
        LambdaQueryWrapper<RankingSnapshot> wrapper = new LambdaQueryWrapper<>();
        if (platform != null && !platform.isEmpty()) {
            wrapper.eq(RankingSnapshot::getPlatform, platform);
        }
        if (rankType != null && !rankType.isEmpty()) {
            wrapper.eq(RankingSnapshot::getRankType, rankType);
        }
        wrapper.eq(RankingSnapshot::getSnapDate, date);
        wrapper.orderByAsc(RankingSnapshot::getRankNo);
        return rankingSnapshotMapper.selectList(wrapper);
    }

    /**
     * 内存分页
     */
    private Page<RankingSnapshot> paginateInMemory(List<RankingSnapshot> allData, int page, int size) {
        int total = allData.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<RankingSnapshot> pageData = start < total
                ? allData.subList(start, end)
                : Collections.emptyList();

        Page<RankingSnapshot> result = new Page<>(page, size);
        result.setRecords(pageData);
        result.setTotal(total);
        return result;
    }

    // ==================== 工具方法 ====================

    private String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private long toLong(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
