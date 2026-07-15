package com.mochao.module.ranking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.module.ranking.dto.RankingQueryDTO;
import com.mochao.module.ranking.dto.ScrapeResult;
import com.mochao.module.ranking.entity.RankingSnapshot;

import java.util.List;
import java.util.Map;

/**
 * 榜单快照服务
 */
public interface RankingService {

    /**
     * 触发抓取并保存到数据库（已有当天数据则跳过）
     */
    ScrapeResult triggerScrape(String platform, String rankType);

    /**
     * 查询已保存的快照（优先 Redis 缓存）
     */
    Page<RankingSnapshot> querySnapshots(RankingQueryDTO dto);

    /**
     * 检查当天是否已有数据（先查 Redis 标记，再回退 DB）
     */
    boolean checkTodayData(String platform, String rankType);

    /**
     * 获取某平台+榜单有数据的日期列表（用于前端日期选择器）
     */
    List<String> getAvailableDates(String platform, String rankType);

    /**
     * 按书名/作者关键词搜索（调用番茄小说实时搜索 API，仿 fanqienovel-downloader）
     * @return 搜索结果列表，每项包含 bookName/author/platform/category/wordCount/coverUrl/bookUrl/bookId 等
     */
    List<Map<String, Object>> searchBooks(String keyword, String platform, int limit);

    /**
     * 清洗历史乱码数据：删除 bookName/author 含 PUA 区字符的快照记录，
     * 并清除对应的 Redis 缓存
     * @return 清洗统计（删除条数、受影响平台/榜单类型）
     */
    Map<String, Object> cleanGarbledData();

    /**
     * 检测某平台+榜单的当天数据是否包含乱码
     * @return true 表示有乱码记录需要清洗
     */
    boolean hasGarbledData(String platform, String rankType);

    /**
     * 强制重新抓取（覆盖当天已有数据，即使已存在也重新抓取）
     * 用于自愈场景：检测到乱码数据后，清洗并重新抓取干净数据
     */
    ScrapeResult forceReScrape(String platform, String rankType);

    /**
     * 异步自愈：清洗乱码数据后重新抓取
     * 被查询层触发，不阻塞用户请求
     */
    void selfHealAsync(String platform, String rankType);
}
