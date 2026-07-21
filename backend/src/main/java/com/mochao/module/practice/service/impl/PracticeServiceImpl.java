package com.mochao.module.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.constant.Constants;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.book.entity.Book;
import com.mochao.module.book.entity.BookChapter;
import com.mochao.module.book.mapper.BookChapterMapper;
import com.mochao.module.book.mapper.BookMapper;
import com.mochao.module.practice.dto.PracticeCompleteDTO;
import com.mochao.module.practice.dto.PracticeProgressDTO;
import com.mochao.module.practice.dto.PracticeStartDTO;
import com.mochao.module.practice.entity.PracticeSession;
import com.mochao.module.practice.mapper.PracticeSessionMapper;
import com.mochao.module.practice.service.PracticeService;
import com.mochao.module.statistics.entity.DailyStatistics;
import com.mochao.module.statistics.mapper.DailyStatisticsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PracticeServiceImpl implements PracticeService {

    private final PracticeSessionMapper practiceSessionMapper;
    private final BookMapper bookMapper;
    private final BookChapterMapper bookChapterMapper;
    private final DailyStatisticsMapper dailyStatisticsMapper;

    public PracticeServiceImpl(PracticeSessionMapper practiceSessionMapper,
                               BookMapper bookMapper,
                               BookChapterMapper bookChapterMapper,
                               DailyStatisticsMapper dailyStatisticsMapper) {
        this.practiceSessionMapper = practiceSessionMapper;
        this.bookMapper = bookMapper;
        this.bookChapterMapper = bookChapterMapper;
        this.dailyStatisticsMapper = dailyStatisticsMapper;
    }

    @Override
    public PracticeSession startPractice(PracticeStartDTO dto, Long userId) {
        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }

        // 确定练习内容长度和章节信息
        int totalChars;
        String chapterTitle = null;
        if (dto.getChapterIndex() != null) {
            // 章节练习：从章节表获取对应章节
            BookChapter chapter = bookChapterMapper.selectOne(
                    new LambdaQueryWrapper<BookChapter>()
                            .eq(BookChapter::getBookId, dto.getBookId())
                            .eq(BookChapter::getChapterIdx, dto.getChapterIndex())
                            .last("LIMIT 1"));
            if (chapter != null) {
                totalChars = chapter.getContent() != null ? chapter.getContent().length() : 0;
                chapterTitle = chapter.getTitle();
            } else {
                // 章节不存在，回退到全书
                totalChars = book.getContent() != null ? book.getContent().length() : 0;
            }
        } else {
            // 整本练习
            totalChars = book.getContent() != null ? book.getContent().length() : 0;
        }

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setBookId(dto.getBookId());
        session.setChapterIndex(dto.getChapterIndex());
        session.setChapterTitle(chapterTitle);
        session.setMode(dto.getMode() != null ? dto.getMode() : "copy");
        session.setStatus(Constants.PRACTICE_STATUS_ACTIVE);
        session.setTotalChars(totalChars);
        session.setTypedContent("");
        session.setCurrentPosition(0);
        session.setTypedChars(0);
        session.setErrorCount(0);
        session.setStartTime(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        practiceSessionMapper.insert(session);
        return session;
    }

    @Override
    public PracticeSession updateProgress(Long id, PracticeProgressDTO dto, Long userId) {
        PracticeSession session = getOwnedSession(id, userId);
        if (!Constants.PRACTICE_STATUS_ACTIVE.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "练习未在进行中");
        }

        session.setTypedContent(dto.getTypedContent());
        session.setCurrentPosition(dto.getCurrentPosition());
        session.setTypedChars(dto.getTypedChars());
        session.setErrorCount(dto.getErrorCount());
        if (dto.getDuration() != null) {
            session.setDuration(dto.getDuration());
        }
        if (dto.getSummaryContent() != null) {
            session.setSummaryContent(dto.getSummaryContent());
        }
        if (dto.getSelfWrittenContent() != null) {
            session.setSelfWrittenContent(dto.getSelfWrittenContent());
        }
        session.setUpdatedAt(LocalDateTime.now());
        practiceSessionMapper.updateById(session);
        return session;
    }

    @Override
    public PracticeSession pausePractice(Long id, Long userId) {
        PracticeSession session = getOwnedSession(id, userId);
        if (!Constants.PRACTICE_STATUS_ACTIVE.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "练习未在进行中");
        }
        session.setStatus(Constants.PRACTICE_STATUS_PAUSED);
        session.setUpdatedAt(LocalDateTime.now());
        practiceSessionMapper.updateById(session);
        return session;
    }

    @Override
    public PracticeSession resumePractice(Long id, Long userId) {
        PracticeSession session = getOwnedSession(id, userId);
        if (!Constants.PRACTICE_STATUS_PAUSED.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "练习未在暂停中");
        }
        session.setStatus(Constants.PRACTICE_STATUS_ACTIVE);
        session.setUpdatedAt(LocalDateTime.now());
        practiceSessionMapper.updateById(session);
        return session;
    }

    @Override
    public PracticeSession completePractice(Long id, PracticeCompleteDTO dto, Long userId) {
        PracticeSession session = getOwnedSession(id, userId);
        session.setStatus(Constants.PRACTICE_STATUS_COMPLETED);
        session.setTypedContent(dto.getTypedContent());
        session.setTypedChars(dto.getTypedChars());
        session.setErrorCount(dto.getErrorCount());
        session.setDuration(dto.getDuration());
        session.setAccuracy(dto.getAccuracy());
        // 钳制 speed：防止 NaN/Infinity/超大值导致 MySQL DECIMAL 溢出
        Double speed = dto.getSpeed();
        if (speed == null || speed.isNaN() || speed.isInfinite() || speed < 0) {
            speed = 0.0;
        }
        speed = Math.min(speed, 99999.99);
        session.setSpeed(speed);
        session.setEndTime(LocalDateTime.now());
        session.setScore(calculateScore(dto.getAccuracy(), speed));
        if (dto.getSummaryContent() != null) {
            session.setSummaryContent(dto.getSummaryContent());
        }
        if (dto.getSelfWrittenContent() != null) {
            session.setSelfWrittenContent(dto.getSelfWrittenContent());
        }
        session.setUpdatedAt(LocalDateTime.now());
        practiceSessionMapper.updateById(session);

        // 更新每日统计
        updateDailyStatistics(userId, session);

        return session;
    }

    @Override
    public PracticeSession getActivePractice(Long userId) {
        return practiceSessionMapper.selectOne(
                new LambdaQueryWrapper<PracticeSession>()
                        .eq(PracticeSession::getUserId, userId)
                        .in(PracticeSession::getStatus,
                                Constants.PRACTICE_STATUS_ACTIVE,
                                Constants.PRACTICE_STATUS_PAUSED)
                        .orderByDesc(PracticeSession::getCreatedAt)
                        .last("LIMIT 1"));
    }

    @Override
    public Page<PracticeSession> getPracticeHistory(Long userId, Integer page, Integer size) {
        Page<PracticeSession> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<PracticeSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PracticeSession::getUserId, userId)
                .eq(PracticeSession::getStatus, Constants.PRACTICE_STATUS_COMPLETED)
                .orderByDesc(PracticeSession::getCreatedAt);
        return practiceSessionMapper.selectPage(pageObj, wrapper);
    }

    private PracticeSession getOwnedSession(Long id, Long userId) {
        PracticeSession session = practiceSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "练习记录不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人练习记录");
        }
        return session;
    }

    private Double calculateScore(Double accuracy, Double speed) {
        if (accuracy == null || speed == null) {
            return 0.0;
        }
        // 综合分 = 准确率 * 0.5 + 速度归一化 * 0.5 (假设速度上限为200字/分钟)
        double speedScore = Math.min(speed / 200.0, 1.0) * 100;
        return accuracy * 0.5 + speedScore * 0.5;
    }

    private void updateDailyStatistics(Long userId, PracticeSession session) {
        LocalDate today = LocalDate.now();
        int typedChars = session.getTypedChars() != null ? session.getTypedChars() : 0;
        int duration = session.getDuration() != null ? session.getDuration() : 0;

        // 重新计算当天所有已完成练习的平均值（按完成时间归类，避免跨天练习归类错误）
        List<PracticeSession> todaySessions = practiceSessionMapper.selectList(
                new LambdaQueryWrapper<PracticeSession>()
                        .eq(PracticeSession::getUserId, userId)
                        .eq(PracticeSession::getStatus, Constants.PRACTICE_STATUS_COMPLETED)
                        .ge(PracticeSession::getEndTime, today.atStartOfDay()));
        double avgAcc = todaySessions.stream()
                .filter(s -> s.getAccuracy() != null)
                .mapToDouble(PracticeSession::getAccuracy)
                .average().orElse(0.0);
        double avgSpd = todaySessions.stream()
                .filter(s -> s.getSpeed() != null)
                .mapToDouble(PracticeSession::getSpeed)
                .average().orElse(0.0);

        // 先尝试原子更新（practice_count / total_chars / total_duration 使用 SQL 原子加法，避免竞态）
        int rows = dailyStatisticsMapper.update(null,
                new LambdaUpdateWrapper<DailyStatistics>()
                        .eq(DailyStatistics::getUserId, userId)
                        .eq(DailyStatistics::getStatDate, today)
                        .setSql("practice_count = practice_count + 1")
                        .setSql("total_chars = total_chars + " + typedChars)
                        .setSql("total_duration = total_duration + " + duration)
                        .set(DailyStatistics::getAvgAccuracy, avgAcc)
                        .set(DailyStatistics::getAvgSpeed, avgSpd)
                        .set(DailyStatistics::getUpdatedAt, LocalDateTime.now()));

        // 更新 0 行说明记录不存在，执行插入（有唯一索引 uk_user_date 兜底）
        if (rows == 0) {
            DailyStatistics stat = new DailyStatistics();
            stat.setUserId(userId);
            stat.setStatDate(today);
            stat.setPracticeCount(1);
            stat.setTotalChars(typedChars);
            stat.setTotalDuration(duration);
            stat.setAvgAccuracy(avgAcc);
            stat.setAvgSpeed(avgSpd);
            stat.setCreatedAt(LocalDateTime.now());
            stat.setUpdatedAt(LocalDateTime.now());
            try {
                dailyStatisticsMapper.insert(stat);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发插入冲突，回退到原子更新
                dailyStatisticsMapper.update(null,
                        new LambdaUpdateWrapper<DailyStatistics>()
                                .eq(DailyStatistics::getUserId, userId)
                                .eq(DailyStatistics::getStatDate, today)
                                .setSql("practice_count = practice_count + 1")
                                .setSql("total_chars = total_chars + " + typedChars)
                                .setSql("total_duration = total_duration + " + duration)
                                .set(DailyStatistics::getAvgAccuracy, avgAcc)
                                .set(DailyStatistics::getAvgSpeed, avgSpd)
                                .set(DailyStatistics::getUpdatedAt, LocalDateTime.now()));
            }
        }
    }
}
