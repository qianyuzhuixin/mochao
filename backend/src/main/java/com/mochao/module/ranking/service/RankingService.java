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
}
