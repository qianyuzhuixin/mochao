package com.mochao.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochao.common.constant.Constants;
import com.mochao.module.practice.entity.PracticeSession;
import com.mochao.module.practice.mapper.PracticeSessionMapper;
import com.mochao.module.statistics.entity.CheckIn;
import com.mochao.module.statistics.entity.DailyStatistics;
import com.mochao.module.statistics.mapper.CheckInMapper;
import com.mochao.module.statistics.mapper.DailyStatisticsMapper;
import com.mochao.module.statistics.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final DailyStatisticsMapper dailyStatisticsMapper;
    private final CheckInMapper checkInMapper;
    private final PracticeSessionMapper practiceSessionMapper;

    public StatisticsServiceImpl(DailyStatisticsMapper dailyStatisticsMapper,
                                 CheckInMapper checkInMapper,
                                 PracticeSessionMapper practiceSessionMapper) {
        this.dailyStatisticsMapper = dailyStatisticsMapper;
        this.checkInMapper = checkInMapper;
        this.practiceSessionMapper = practiceSessionMapper;
    }

    @Override
    public Map<String, Object> getOverview(Long userId) {
        Map<String, Object> overview = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 今日统计
        DailyStatistics todayStat = dailyStatisticsMapper.selectOne(
                new LambdaQueryWrapper<DailyStatistics>()
                        .eq(DailyStatistics::getUserId, userId)
                        .eq(DailyStatistics::getStatDate, today));

        int todayChars = todayStat != null ? todayStat.getTotalChars() : 0;
        int todayPracticeCount = todayStat != null ? todayStat.getPracticeCount() : 0;
        int todayDuration = todayStat != null ? todayStat.getTotalDuration() : 0;
        double todayAccuracy = todayStat != null ? todayStat.getAvgAccuracy() : 0.0;
        double todaySpeed = todayStat != null ? todayStat.getAvgSpeed() : 0.0;

        overview.put("todayChars", todayChars);
        overview.put("todayWords", todayChars);        // 前端兼容
        overview.put("todayPracticeCount", todayPracticeCount);
        overview.put("todayDuration", todayDuration);
        overview.put("todayAccuracy", todayAccuracy);
        overview.put("todaySpeed", todaySpeed);

        // 总统计
        List<DailyStatistics> allStats = dailyStatisticsMapper.selectList(
                new LambdaQueryWrapper<DailyStatistics>().eq(DailyStatistics::getUserId, userId));
        int totalPractice = allStats.stream().mapToInt(DailyStatistics::getPracticeCount).sum();
        int totalChars = allStats.stream().mapToInt(DailyStatistics::getTotalChars).sum();
        int totalDuration = allStats.stream().mapToInt(DailyStatistics::getTotalDuration).sum();
        overview.put("totalPractice", totalPractice);
        overview.put("totalSessions", totalPractice);    // 前端兼容
        overview.put("totalChars", totalChars);
        overview.put("totalWords", totalChars);          // 前端兼容
        overview.put("totalDuration", totalDuration);

        // 连续打卡天数
        CheckIn latestCheckIn = checkInMapper.selectOne(
                new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .orderByDesc(CheckIn::getCheckDate)
                        .last("LIMIT 1"));
        overview.put("streakDays", latestCheckIn != null ? latestCheckIn.getStreakDays() : 0);

        // 今日是否已打卡
        CheckIn todayCheckIn = checkInMapper.selectOne(
                new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .eq(CheckIn::getCheckDate, today));
        overview.put("checkedIn", todayCheckIn != null);

        return overview;
    }

    @Override
    public Map<String, Object> getTrend(Long userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);

        List<DailyStatistics> stats = dailyStatisticsMapper.selectList(
                new LambdaQueryWrapper<DailyStatistics>()
                        .eq(DailyStatistics::getUserId, userId)
                        .ge(DailyStatistics::getStatDate, startDate)
                        .le(DailyStatistics::getStatDate, endDate)
                        .orderByAsc(DailyStatistics::getStatDate));

        List<String> dates = new ArrayList<>();
        List<Integer> practiceCounts = new ArrayList<>();
        List<Integer> totalChars = new ArrayList<>();
        List<Integer> totalDurations = new ArrayList<>();
        List<Double> avgAccuracies = new ArrayList<>();
        List<Double> avgSpeeds = new ArrayList<>();

        // 填充缺失日期
        Map<LocalDate, DailyStatistics> statMap = stats.stream()
                .collect(Collectors.toMap(DailyStatistics::getStatDate, s -> s, (a, b) -> a));

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dates.add(date.toString());
            DailyStatistics stat = statMap.get(date);
            practiceCounts.add(stat != null ? stat.getPracticeCount() : 0);
            totalChars.add(stat != null ? stat.getTotalChars() : 0);
            totalDurations.add(stat != null ? stat.getTotalDuration() : 0);
            avgAccuracies.add(stat != null ? stat.getAvgAccuracy() : 0.0);
            avgSpeeds.add(stat != null ? stat.getAvgSpeed() : 0.0);
        }

        result.put("dates", dates);
        result.put("practiceCounts", practiceCounts);
        result.put("totalChars", totalChars);
        result.put("totalDurations", totalDurations);
        result.put("avgAccuracies", avgAccuracies);
        result.put("avgSpeeds", avgSpeeds);

        return result;
    }

    @Override
    public Map<String, Object> checkIn(Long userId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new HashMap<>();

        // 检查今天是否已打卡
        CheckIn todayCheckIn = checkInMapper.selectOne(
                new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .eq(CheckIn::getCheckDate, today));

        if (todayCheckIn != null) {
            result.put("checkedIn", true);
            result.put("streakDays", todayCheckIn.getStreakDays());
            return result;
        }

        // 查询昨天打卡记录
        CheckIn yesterdayCheckIn = checkInMapper.selectOne(
                new LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, userId)
                        .eq(CheckIn::getCheckDate, today.minusDays(1)));

        int streakDays = yesterdayCheckIn != null ? yesterdayCheckIn.getStreakDays() + 1 : 1;

        CheckIn newCheckIn = new CheckIn();
        newCheckIn.setUserId(userId);
        newCheckIn.setCheckDate(today);
        newCheckIn.setStreakDays(streakDays);
        newCheckIn.setCreatedAt(LocalDateTime.now());
        checkInMapper.insert(newCheckIn);

        result.put("checkedIn", true);
        result.put("streakDays", streakDays);
        return result;
    }

    @Override
    public Map<String, Object> getCalendar(Long userId, Integer year, Integer month) {
        Map<String, Object> result = new HashMap<>();

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        List<DailyStatistics> stats = dailyStatisticsMapper.selectList(
                new LambdaQueryWrapper<DailyStatistics>()
                        .eq(DailyStatistics::getUserId, userId)
                        .ge(DailyStatistics::getStatDate, startOfMonth)
                        .le(DailyStatistics::getStatDate, endOfMonth)
                        .orderByAsc(DailyStatistics::getStatDate));

        List<Map<String, Object>> calendar = new ArrayList<>();
        for (DailyStatistics stat : stats) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", stat.getStatDate().toString());
            day.put("practiceCount", stat.getPracticeCount());
            day.put("totalChars", stat.getTotalChars());
            day.put("totalDuration", stat.getTotalDuration());
            calendar.add(day);
        }

        result.put("year", year);
        result.put("month", month);
        result.put("calendar", calendar);
        return result;
    }
}
