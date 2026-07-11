package com.mochao.module.statistics.controller;

import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.statistics.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(statisticsService.getOverview(userId));
    }

    @GetMapping("/trend")
    public Result<Map<String, Object>> trend() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(statisticsService.getTrend(userId));
    }

    @GetMapping("/check-in")
    public Result<Map<String, Object>> checkIn() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(statisticsService.checkIn(userId));
    }

    @GetMapping("/calendar")
    public Result<Map<String, Object>> calendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (month == null) {
            month = LocalDate.now().getMonthValue();
        }
        return Result.success(statisticsService.getCalendar(userId, year, month));
    }
}
