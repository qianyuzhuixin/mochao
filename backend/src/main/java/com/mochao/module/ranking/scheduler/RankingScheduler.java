package com.mochao.module.ranking.scheduler;

import com.mochao.module.ranking.dto.ScrapeResult;
import com.mochao.module.ranking.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 榜单数据定时抓取 —— 每天凌晨 3:00 自动抓取所有平台榜单
 */
@Component
public class RankingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RankingScheduler.class);

    private final RankingService rankingService;

    /** 当前支持的自动抓取任务（scraper 支持的平台+榜单组合） */
    private static final String[][] AUTO_SCRAPE_TASKS = {
            // === 起点 (10个) ===
            {"qidian", "month_ticket"},
            {"qidian", "recommend"},
            {"qidian", "collect"},
            {"qidian", "hotsales"},
            {"qidian", "readindex"},
            {"qidian", "signnewbook"},
            {"qidian", "newauthor"},
            {"qidian", "pubnewbook"},
            {"qidian", "sanjiang"},
            // === 番茄 — 综合榜 ===
            {"fanqie", "hot_search"},
            {"fanqie", "read_rank"},
            {"fanqie", "new_book"},
            // 番茄 — 热门分类榜（每频道选几个热门分类）
            {"fanqie", "male-258-hot_search"},   // 传统玄幻
            {"fanqie", "male-1140-hot_search"},  // 东方仙侠
            {"fanqie", "male-261-hot_search"},   // 都市日常
            {"fanqie", "female-248-hot_search"}, // 玄幻言情
            {"fanqie", "female-1139-hot_search"},// 古风世情
            // === 晋江 (5个) ===
            {"jinjiang", "income12"},  // 收入金榜
            {"jinjiang", "month7"},    // 月榜
            {"jinjiang", "season8"},   // 季度榜
            {"jinjiang", "finish14"},  // 完结金榜
            {"jinjiang", "collect"},   // 收藏榜(bookbase)
            // === 七猫 (4个热门榜) ===
            {"qimao", "boy-hot"},      // 男频大热榜
            {"qimao", "boy-new"},      // 男频新书榜
            {"qimao", "girl-hot"},     // 女频大热榜
            {"qimao", "girl-new"},     // 女频新书榜
            // === 刺猬猫 (4个热门榜) ===
            {"ciweimao", "click"},     // 点击榜
            {"ciweimao", "favor"},     // 收藏榜
            {"ciweimao", "monthly"},   // 月票榜
            {"ciweimao", "newbook"},   // 新书榜
    };

    public RankingScheduler(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * 每天凌晨 3:00 执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoScrapeDaily() {
        log.info("========= 定时抓取任务开始（{}）=========", java.time.LocalDateTime.now());

        int successCount = 0;
        int failCount = 0;

        for (String[] task : AUTO_SCRAPE_TASKS) {
            String platform = task[0];
            String rankType = task[1];
            try {
                ScrapeResult result = rankingService.triggerScrape(platform, rankType);
                if (result.isSuccess()) {
                    successCount++;
                    log.info("[定时] {} {} 抓取成功，共 {} 条", platform, rankType, result.getCount());
                } else {
                    failCount++;
                    log.warn("[定时] {} {} 抓取失败: {}", platform, rankType, result.getError());
                }
            } catch (Exception e) {
                failCount++;
                log.error("[定时] {} {} 抓取异常", platform, rankType, e);
            }
        }

        log.info("========= 定时抓取任务完成：成功 {} / 失败 {} =========", successCount, failCount);
    }
}
