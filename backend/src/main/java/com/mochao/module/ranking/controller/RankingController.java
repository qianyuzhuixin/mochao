package com.mochao.module.ranking.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.constant.Constants;
import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.book.dto.BookCreateDTO;
import com.mochao.module.book.dto.ChapterItem;
import com.mochao.module.book.entity.Book;
import com.mochao.module.book.entity.BookChapter;
import com.mochao.module.book.mapper.BookChapterMapper;
import com.mochao.module.book.mapper.BookMapper;
import com.mochao.module.book.service.BookService;
import com.mochao.module.ranking.client.ScraperClient;
import com.mochao.module.ranking.dto.RankingQueryDTO;
import com.mochao.module.ranking.dto.ScrapeResult;
import com.mochao.module.ranking.entity.RankingSnapshot;
import com.mochao.module.ranking.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网文榜单接口
 */
@RestController
@RequestMapping("/v1/ranking")
public class RankingController {

    private static final Logger log = LoggerFactory.getLogger(RankingController.class);

    private final RankingService rankingService;
    private final ScraperClient scraperClient;
    private final BookService bookService;
    private final BookMapper bookMapper;
    private final BookChapterMapper bookChapterMapper;

    public RankingController(RankingService rankingService,
                              ScraperClient scraperClient,
                              BookService bookService,
                              BookMapper bookMapper,
                              BookChapterMapper bookChapterMapper) {
        this.rankingService = rankingService;
        this.scraperClient = scraperClient;
        this.bookService = bookService;
        this.bookMapper = bookMapper;
        this.bookChapterMapper = bookChapterMapper;
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

    /**
     * 下载整本小说到素材库
     * @param body { platform, bookId, target: "personal"|"library", maxChapters? }
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/download-book")
    public Result<Map<String, Object>> downloadBook(@RequestBody Map<String, Object> body) {
        String platform = (String) body.get("platform");
        String bookId = (String) body.get("bookId");
        String target = (String) body.getOrDefault("target", "personal");
        int maxChapters = body.containsKey("maxChapters") && body.get("maxChapters") != null
                ? ((Number) body.get("maxChapters")).intValue() : 0;

        if (platform == null || bookId == null) {
            return Result.error(400, "缺少参数: platform, bookId");
        }

        if (!"fanqie".equals(platform)) {
            return Result.error(400, "暂不支持该平台的整本下载");
        }

        // 管理员才能下载到内置书库
        if ("library".equals(target) && !SecurityUtils.isAdmin()) {
            return Result.error(403, "无权限：仅管理员可下载到内置书库");
        }

        log.info("下载小说: platform={}, bookId={}, target={}, maxChapters={}", platform, bookId, target, maxChapters);

        // 1. 调用 scraper 下载
        Map<String, Object> downloadResult = scraperClient.downloadBook(platform, bookId, maxChapters);
        if (!Boolean.TRUE.equals(downloadResult.get("success"))) {
            String error = (String) downloadResult.getOrDefault("error", "下载失败");
            String detail = (String) downloadResult.get("detail");
            return Result.error(500, detail != null ? error + ": " + detail : error);
        }

        // 2. 构造 BookCreateDTO
        String bookName = (String) downloadResult.get("bookName");
        String author = (String) downloadResult.get("author");
        String category = (String) downloadResult.get("category");
        String fullText = (String) downloadResult.get("fullText");

        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle(bookName);
        dto.setBookName(bookName);
        dto.setAuthor(author);
        dto.setCategory(category);
        dto.setContent(fullText);
        dto.setDifficulty("medium");

        // 构造章节列表
        List<Map<String, Object>> chapterList = (List<Map<String, Object>>) downloadResult.get("chapters");
        if (chapterList != null && !chapterList.isEmpty()) {
            List<ChapterItem> chapters = new ArrayList<>();
            for (int i = 0; i < chapterList.size(); i++) {
                Map<String, Object> ch = chapterList.get(i);
                ChapterItem item = new ChapterItem();
                item.setIndex(i);
                item.setTitle((String) ch.get("title"));
                item.setContent((String) ch.get("content"));
                Object wc = ch.get("wordCount");
                item.setWordCount(wc != null ? ((Number) wc).intValue() : 0);
                chapters.add(item);
            }
            dto.setChapters(chapters);
        }

        // 3. 保存到数据库
        Long userId = SecurityUtils.getCurrentUserId();
        Book savedBook;
        if ("library".equals(target)) {
            savedBook = createBuiltinBook(dto, userId);
        } else {
            savedBook = bookService.createBook(dto, userId);
        }

        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("bookId", savedBook.getId());
        result.put("bookName", savedBook.getBookName());
        result.put("author", savedBook.getAuthor());
        result.put("totalChapters", downloadResult.get("totalChapters"));
        result.put("downloadedChapters", downloadResult.get("downloadedChapters"));
        result.put("successCount", downloadResult.get("successCount"));
        result.put("totalWords", downloadResult.get("totalWords"));
        result.put("target", target);
        result.put("message", String.format("《%s》已下载到%s（%s章/%s字）",
                bookName,
                "library".equals(target) ? "内置书库" : "个人素材",
                downloadResult.get("downloadedChapters"),
                downloadResult.get("totalWords")));

        log.info("下载完成: {} → {} (bookId={})", bookName, target, savedBook.getId());
        return Result.success(result);
    }

    /**
     * 创建内置书库书籍 (sourceType=0)
     */
    private Book createBuiltinBook(BookCreateDTO dto, Long adminId) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setBookName(dto.getBookName());
        book.setAuthor(dto.getAuthor());
        book.setCategory(dto.getCategory());
        book.setTags(dto.getTags());
        book.setContent(dto.getContent());
        book.setDifficulty(2); // medium
        book.setWordCount(dto.getContent() != null ? dto.getContent().length() : 0);
        book.setSourceType(Constants.SOURCE_TYPE_BUILTIN);
        book.setCreatorId(adminId);
        book.setStatus(1);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.insert(book);

        // 保存章节
        if (dto.getChapters() != null && !dto.getChapters().isEmpty()) {
            for (int i = 0; i < dto.getChapters().size(); i++) {
                ChapterItem ch = dto.getChapters().get(i);
                BookChapter chapter = new BookChapter();
                chapter.setBookId(book.getId());
                chapter.setChapterIdx(ch.getIndex() != null ? ch.getIndex() : i);
                chapter.setTitle(ch.getTitle());
                chapter.setContent(ch.getContent());
                chapter.setWordCount(ch.getWordCount() != null ? ch.getWordCount() : 0);
                chapter.setCreatedAt(LocalDateTime.now());
                bookChapterMapper.insert(chapter);
            }
        }

        return book;
    }
}
