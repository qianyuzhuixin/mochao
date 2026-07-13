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
            // === 起点 (10个) ===            {"qidian", "month_ticket"},
            {"qidian", "recommend"},
            {"qidian", "collect"},
            {"qidian", "hotsales"},
            {"qidian", "readindex"},
            {"qidian", "signnewbook"},
            {"qidian", "newauthor"},
            {"qidian", "pubnewbook"},
            {"qidian", "sanjiang"},
            {"qidian", "newsign"},     // 新人签约新书榜
            // === 番茄综合榜 (3) + 全分类 (111) ===
            {"fanqie", "hot_search"},
            {"fanqie", "read_rank"},
            {"fanqie", "new_book"},
            // 番茄·男频·热搜榜
            {"fanqie", "male-8-hot_search"},
            {"fanqie", "male-27-hot_search"},
            {"fanqie", "male-124-hot_search"},
            {"fanqie", "male-257-hot_search"},
            {"fanqie", "male-258-hot_search"},
            {"fanqie", "male-261-hot_search"},
            {"fanqie", "male-262-hot_search"},
            {"fanqie", "male-263-hot_search"},
            {"fanqie", "male-272-hot_search"},
            {"fanqie", "male-273-hot_search"},
            {"fanqie", "male-504-hot_search"},
            {"fanqie", "male-539-hot_search"},
            {"fanqie", "male-718-hot_search"},
            {"fanqie", "male-746-hot_search"},
            {"fanqie", "male-751-hot_search"},
            {"fanqie", "male-1014-hot_search"},
            {"fanqie", "male-1016-hot_search"},
            {"fanqie", "male-1140-hot_search"},
            {"fanqie", "male-1141-hot_search"},
            // 番茄·女频·热搜榜
            {"fanqie", "female-8-hot_search"},
            {"fanqie", "female-23-hot_search"},
            {"fanqie", "female-24-hot_search"},
            {"fanqie", "female-79-hot_search"},
            {"fanqie", "female-246-hot_search"},
            {"fanqie", "female-248-hot_search"},
            {"fanqie", "female-253-hot_search"},
            {"fanqie", "female-267-hot_search"},
            {"fanqie", "female-539-hot_search"},
            {"fanqie", "female-745-hot_search"},
            {"fanqie", "female-746-hot_search"},
            {"fanqie", "female-747-hot_search"},
            {"fanqie", "female-748-hot_search"},
            {"fanqie", "female-749-hot_search"},
            {"fanqie", "female-750-hot_search"},
            {"fanqie", "female-1015-hot_search"},
            {"fanqie", "female-1017-hot_search"},
            {"fanqie", "female-1139-hot_search"},
            // 番茄·男频·阅读榜
            {"fanqie", "male-8-read_rank"},
            {"fanqie", "male-27-read_rank"},
            {"fanqie", "male-124-read_rank"},
            {"fanqie", "male-257-read_rank"},
            {"fanqie", "male-258-read_rank"},
            {"fanqie", "male-261-read_rank"},
            {"fanqie", "male-262-read_rank"},
            {"fanqie", "male-263-read_rank"},
            {"fanqie", "male-272-read_rank"},
            {"fanqie", "male-273-read_rank"},
            {"fanqie", "male-504-read_rank"},
            {"fanqie", "male-539-read_rank"},
            {"fanqie", "male-718-read_rank"},
            {"fanqie", "male-746-read_rank"},
            {"fanqie", "male-751-read_rank"},
            {"fanqie", "male-1014-read_rank"},
            {"fanqie", "male-1016-read_rank"},
            {"fanqie", "male-1140-read_rank"},
            {"fanqie", "male-1141-read_rank"},
            // 番茄·女频·阅读榜
            {"fanqie", "female-8-read_rank"},
            {"fanqie", "female-23-read_rank"},
            {"fanqie", "female-24-read_rank"},
            {"fanqie", "female-79-read_rank"},
            {"fanqie", "female-246-read_rank"},
            {"fanqie", "female-248-read_rank"},
            {"fanqie", "female-253-read_rank"},
            {"fanqie", "female-267-read_rank"},
            {"fanqie", "female-539-read_rank"},
            {"fanqie", "female-745-read_rank"},
            {"fanqie", "female-746-read_rank"},
            {"fanqie", "female-747-read_rank"},
            {"fanqie", "female-748-read_rank"},
            {"fanqie", "female-749-read_rank"},
            {"fanqie", "female-750-read_rank"},
            {"fanqie", "female-1015-read_rank"},
            {"fanqie", "female-1017-read_rank"},
            {"fanqie", "female-1139-read_rank"},
            // 番茄·男频·新书榜
            {"fanqie", "male-8-new_book"},
            {"fanqie", "male-27-new_book"},
            {"fanqie", "male-124-new_book"},
            {"fanqie", "male-257-new_book"},
            {"fanqie", "male-258-new_book"},
            {"fanqie", "male-261-new_book"},
            {"fanqie", "male-262-new_book"},
            {"fanqie", "male-263-new_book"},
            {"fanqie", "male-272-new_book"},
            {"fanqie", "male-273-new_book"},
            {"fanqie", "male-504-new_book"},
            {"fanqie", "male-539-new_book"},
            {"fanqie", "male-718-new_book"},
            {"fanqie", "male-746-new_book"},
            {"fanqie", "male-751-new_book"},
            {"fanqie", "male-1014-new_book"},
            {"fanqie", "male-1016-new_book"},
            {"fanqie", "male-1140-new_book"},
            {"fanqie", "male-1141-new_book"},
            // 番茄·女频·新书榜
            {"fanqie", "female-8-new_book"},
            {"fanqie", "female-23-new_book"},
            {"fanqie", "female-24-new_book"},
            {"fanqie", "female-79-new_book"},
            {"fanqie", "female-246-new_book"},
            {"fanqie", "female-248-new_book"},
            {"fanqie", "female-253-new_book"},
            {"fanqie", "female-267-new_book"},
            {"fanqie", "female-539-new_book"},
            {"fanqie", "female-745-new_book"},
            {"fanqie", "female-746-new_book"},
            {"fanqie", "female-747-new_book"},
            {"fanqie", "female-748-new_book"},
            {"fanqie", "female-749-new_book"},
            {"fanqie", "female-750-new_book"},
            {"fanqie", "female-1015-new_book"},
            {"fanqie", "female-1017-new_book"},
            {"fanqie", "female-1139-new_book"},
            // === 晋江 (7个) ===
            {"jinjiang", "income12"},  // 收入金榜
            {"jinjiang", "month7"},    // 月榜
            {"jinjiang", "season8"},   // 季度榜
            {"jinjiang", "finish14"},  // 完结金榜
            {"jinjiang", "collect"},   // 收藏榜(bookbase)
            {"jinjiang", "new15"},     // 新手金榜
            {"jinjiang", "kzi17"},     // 千字金榜
            // === 七猫 (10个) ===
            {"qimao", "boy-hot"},      // 男频大热榜
            {"qimao", "boy-new"},      // 男频新书榜
            {"qimao", "girl-hot"},     // 女频大热榜
            {"qimao", "girl-new"},     // 女频新书榜
            {"qimao", "boy-over"},     // 男频完结榜
            {"qimao", "boy-collect"},  // 男频收藏榜
            {"qimao", "boy-update"},   // 男频更新榜
            {"qimao", "girl-over"},    // 女频完结榜
            {"qimao", "girl-collect"}, // 女频收藏榜
            {"qimao", "girl-update"},  // 女频更新榜
            // === 刺猬猫 (9个) ===
            {"ciweimao", "click"},     // 点击榜
            {"ciweimao", "favor"},     // 收藏榜
            {"ciweimao", "monthly"},   // 月票榜
            {"ciweimao", "newbook"},   // 新书榜
            {"ciweimao", "recommend"}, // 推荐榜
            {"ciweimao", "subscribe"}, // 订阅榜
            {"ciweimao", "tsukkomi"},  // 吐槽榜
            {"ciweimao", "blade"},     // 刀片榜
            {"ciweimao", "update"},    // 更新榜
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
