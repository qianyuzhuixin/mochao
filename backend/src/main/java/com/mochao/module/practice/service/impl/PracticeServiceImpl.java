package com.mochao.module.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        session.setSpeed(dto.getSpeed());
        session.setEndTime(LocalDateTime.now());
        session.setScore(calculateScore(dto.getAccuracy(), dto.getSpeed()));
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
        DailyStatistics stat = dailyStatisticsMapper.selectOne(
                new LambdaQueryWrapper<DailyStatistics>()
                        .eq(DailyStatistics::getUserId, userId)
                        .eq(DailyStatistics::getStatDate, today));

        if (stat == null) {
            stat = new DailyStatistics();
            stat.setUserId(userId);
            stat.setStatDate(today);
            stat.setPracticeCount(1);
            stat.setTotalChars(session.getTypedChars() != null ? session.getTypedChars() : 0);
            stat.setTotalDuration(session.getDuration() != null ? session.getDuration() : 0);
            stat.setAvgAccuracy(session.getAccuracy() != null ? session.getAccuracy() : 0.0);
            stat.setAvgSpeed(session.getSpeed() != null ? session.getSpeed() : 0.0);
            stat.setCreatedAt(LocalDateTime.now());
            stat.setUpdatedAt(LocalDateTime.now());
            dailyStatisticsMapper.insert(stat);
        } else {
            stat.setPracticeCount(stat.getPracticeCount() + 1);
            stat.setTotalChars(stat.getTotalChars() + (session.getTypedChars() != null ? session.getTypedChars() : 0));
            stat.setTotalDuration(stat.getTotalDuration() + (session.getDuration() != null ? session.getDuration() : 0));

            // 重新计算平均值
            List<PracticeSession> todaySessions = practiceSessionMapper.selectList(
                    new LambdaQueryWrapper<PracticeSession>()
                            .eq(PracticeSession::getUserId, userId)
                            .eq(PracticeSession::getStatus, Constants.PRACTICE_STATUS_COMPLETED)
                            .ge(PracticeSession::getCreatedAt, today.atStartOfDay()));
            double avgAcc = todaySessions.stream()
                    .filter(s -> s.getAccuracy() != null)
                    .mapToDouble(PracticeSession::getAccuracy)
                    .average().orElse(0.0);
            double avgSpd = todaySessions.stream()
                    .filter(s -> s.getSpeed() != null)
                    .mapToDouble(PracticeSession::getSpeed)
                    .average().orElse(0.0);
            stat.setAvgAccuracy(avgAcc);
            stat.setAvgSpeed(avgSpd);
            stat.setUpdatedAt(LocalDateTime.now());
            dailyStatisticsMapper.updateById(stat);
        }
    }
}
