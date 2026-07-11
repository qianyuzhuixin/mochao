package com.mochao.module.statistics.service;

import java.util.Map;

public interface StatisticsService {

    Map<String, Object> getOverview(Long userId);

    Map<String, Object> getTrend(Long userId);

    Map<String, Object> checkIn(Long userId);

    Map<String, Object> getCalendar(Long userId, Integer year, Integer month);
}
