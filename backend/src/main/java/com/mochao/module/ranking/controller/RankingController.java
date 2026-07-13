package com.mochao.module.ranking.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.result.Result;
import com.mochao.module.ranking.dto.RankingQueryDTO;
import com.mochao.module.ranking.dto.ScrapeResult;
import com.mochao.module.ranking.entity.RankingSnapshot;
import com.mochao.module.ranking.service.RankingService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网文榜单接口
 */
@RestController
@RequestMapping("/v1/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * 触发抓取（用户点击按钮调用，已有当天数据则跳过）
     */
    @PostMapping("/scrape")
    public Result<ScrapeResult> scrape(@RequestParam String platform,
                                       @RequestParam String rankType) {
        ScrapeResult result = rankingService.triggerScrape(platform, rankType);
        if (!result.isSuccess()) {
            return Result.error(500, result.getError() != null ? result.getError() : "抓取失败");
        }
        return Result.success(result);
    }

    /**
     * 查询榜单快照（优先 Redis 缓存，内存分页）
     */
    @GetMapping
    public Result<Page<RankingSnapshot>> query(RankingQueryDTO dto) {
        return Result.success(rankingService.querySnapshots(dto));
    }

    /**
     * 检查当天是否已有数据（前端判断是否显示抓取按钮）
     */
    @GetMapping("/check-today")
    public Result<Map<String, Object>> checkToday(@RequestParam String platform,
                                                   @RequestParam String rankType) {
        boolean exists = rankingService.checkTodayData(platform, rankType);
        Map<String, Object> data = new HashMap<>();
        data.put("exists", exists);
        return Result.success(data);
    }

    /**
     * 获取某平台+榜单有数据的日期列表（用于前端日期选择器）
     */
    @GetMapping("/available-dates")
    public Result<List<String>> availableDates(@RequestParam String platform,
                                                @RequestParam String rankType) {
        return Result.success(rankingService.getAvailableDates(platform, rankType));
    }
}
