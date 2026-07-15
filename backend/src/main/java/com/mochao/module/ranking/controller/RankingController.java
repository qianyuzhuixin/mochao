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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
     * 搜索小说（调用番茄小说实时搜索 API，仿 fanqienovel-downloader）
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> searchBooks(@RequestParam String keyword,
                                                    @RequestParam(defaultValue = "") String platform,
                                                    @RequestParam(defaultValue = "20") int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error(400, "搜索关键词不能为空");
        }
        limit = Math.max(1, Math.min(200, limit));

        log.info("本地搜索: keyword={}, platform={}, limit={}", keyword, platform, limit);

        List<Map<String, Object>> books = rankingService.searchBooks(keyword.trim(),
                platform != null && !platform.isEmpty() ? platform : null, limit);

        Map<String, Object> data = new HashMap<>();
        data.put("books", books);
        data.put("total", books.size());
        data.put("hasMore", false);
        data.put("source", "fanqie-api");

        return Result.success(data);
    }

    /**
     * 清洗历史乱码数据（管理员手动触发 — 兜底操作）
     * 系统已具备自动自愈机制（查询时自动检测+异步清洗+4:00定时巡检），
     * 此端点仅作为管理员兜底手段
     */
    @PostMapping("/clean-garbled")
    public Result<Map<String, Object>> cleanGarbledData() {
        if (!SecurityUtils.isAdmin()) {
            return Result.error(403, "仅管理员可执行乱码清洗");
        }
        Map<String, Object> stats = rankingService.cleanGarbledData();
        return Result.success(stats);
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

        // 构造章节列表 — scraper 返回的 chapters[0] 为"书籍信息"(第0章)，正文从第1章开始
        List<Map<String, Object>> chapterList = (List<Map<String, Object>>) downloadResult.get("chapters");
        if (chapterList != null && !chapterList.isEmpty()) {
            List<ChapterItem> chapters = new ArrayList<>();
            for (int i = 0; i < chapterList.size(); i++) {
                Map<String, Object> ch = chapterList.get(i);
                ChapterItem item = new ChapterItem();
                item.setIndex(i);  // 第0章=书籍信息，第1章=正文第一章
                item.setTitle((String) ch.get("title"));
                item.setContent((String) ch.get("content"));
                Object wc = ch.get("wordCount");
                item.setWordCount(wc != null ? ((Number) wc).intValue() : 0);
                chapters.add(item);
            }
            dto.setChapters(chapters);
        }

        // 中止信息 — 单章失败时返回已成功部分 + 告知用户中止原因
        Boolean aborted = (Boolean) downloadResult.get("aborted");
        String abortReason = (String) downloadResult.getOrDefault("abortReason", "");

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
        String message = String.format("《%s》已下载到%s（%s章/%s字）",
                bookName,
                "library".equals(target) ? "内置书库" : "个人素材",
                downloadResult.get("downloadedChapters"),
                downloadResult.get("totalWords"));
        if (Boolean.TRUE.equals(aborted) && abortReason != null && !abortReason.isEmpty()) {
            message += "（部分下载：" + abortReason + "）";
        }
        result.put("message", message);
        result.put("aborted", aborted);
        result.put("abortReason", abortReason);
        Object failedChapters = downloadResult.get("failedChapters");
        result.put("failedChapters", failedChapters != null ? failedChapters : 0);

        log.info("下载完成: {} → {} (bookId={}, aborted={})", bookName, target, savedBook.getId(), aborted);
        return Result.success(result);
    }

    /**
     * 下载小说文件（TXT / HTML / PDF）
     * 直接返回可下载的文件流，不存入数据库
     *
     * @param bookId 书籍 ID（番茄小说 bookId）
     * @param format 输出格式：txt / html / pdf
     * @param maxChapters 最大下载章节数（0 = 全部，默认 0）
     */
    @GetMapping("/download-file")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String bookId,
                                                @RequestParam(defaultValue = "txt") String format,
                                                @RequestParam(defaultValue = "0") int maxChapters) {
        if (bookId == null || bookId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // 校验格式
        if (!"txt".equals(format) && !"html".equals(format) && !"pdf".equals(format)) {
            return ResponseEntity.badRequest().body(null);
        }

        log.info("下载文件: bookId={}, format={}, maxChapters={}", bookId, format, maxChapters);

        try {
            // 调用 scraper 下载并转换格式
            ResponseEntity<byte[]> scraperResp = scraperClient.downloadFile(
                    "fanqie", bookId.trim(), format, maxChapters);

            byte[] fileBytes = scraperResp.getBody();
            if (fileBytes == null || fileBytes.length == 0) {
                return ResponseEntity.internalServerError().body(null);
            }

            // 从 scraper 响应头获取 Content-Type 和文件名
            HttpHeaders scraperHeaders = scraperResp.getHeaders();
            MediaType contentType = scraperHeaders.getContentType();
            String contentDisposition = scraperHeaders.getFirst(HttpHeaders.CONTENT_DISPOSITION);

            // 构建响应
            HttpHeaders respHeaders = new HttpHeaders();
            if (contentType != null) {
                respHeaders.setContentType(contentType);
            }
            if (contentDisposition != null) {
                respHeaders.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            }
            respHeaders.setContentLength(fileBytes.length);

            log.info("文件下载完成: {} bytes, type={}", fileBytes.length, contentType);

            return ResponseEntity.ok().headers(respHeaders).body(fileBytes);
        } catch (Exception e) {
            log.error("文件下载失败: bookId={}, format={}, error={}", bookId, format, e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
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

        // 批量保存章节
        if (dto.getChapters() != null && !dto.getChapters().isEmpty()) {
            List<BookChapter> entities = new ArrayList<>();
            for (int i = 0; i < dto.getChapters().size(); i++) {
                ChapterItem ch = dto.getChapters().get(i);
                BookChapter chapter = new BookChapter();
                chapter.setBookId(book.getId());
                chapter.setChapterIdx(ch.getIndex() != null ? ch.getIndex() : i);
                chapter.setTitle(ch.getTitle());
                chapter.setContent(ch.getContent());
                chapter.setWordCount(ch.getWordCount() != null ? ch.getWordCount() : 0);
                chapter.setCreatedAt(LocalDateTime.now());
                entities.add(chapter);
            }
            // 分批插入，每批最多 500 条
            int batchSize = 500;
            for (int i = 0; i < entities.size(); i += batchSize) {
                int end = Math.min(i + batchSize, entities.size());
                bookChapterMapper.batchInsert(entities.subList(i, end));
            }
        }

        return book;
    }
}
