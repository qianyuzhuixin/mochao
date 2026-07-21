package com.mochao.module.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.constant.Constants;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.book.dto.BookCreateDTO;
import com.mochao.module.book.dto.BookQueryDTO;
import com.mochao.module.book.dto.ChapterItem;
import com.mochao.module.book.dto.FileParseResult;
import com.mochao.module.book.entity.Book;
import com.mochao.module.book.entity.BookChapter;
import com.mochao.module.book.mapper.BookChapterMapper;
import com.mochao.module.book.mapper.BookMapper;
import com.mochao.module.book.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.mochao.module.book.util.ChapterSplitUtil;

@Service
public class BookServiceImpl implements BookService {

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    private final BookMapper bookMapper;
    private final BookChapterMapper chapterMapper;

    public BookServiceImpl(BookMapper bookMapper, BookChapterMapper chapterMapper) {
        this.bookMapper = bookMapper;
        this.chapterMapper = chapterMapper;
    }

    @Override
    public Page<Book> getBookList(BookQueryDTO dto, Long currentUserId) {
        Page<Book> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        Integer sourceType = dto.getSourceType() != null ? dto.getSourceType() : Constants.SOURCE_TYPE_BUILTIN;

        if (sourceType == Constants.SOURCE_TYPE_BUILTIN) {
            // 内置素材所有人可见
            wrapper.eq(Book::getSourceType, Constants.SOURCE_TYPE_BUILTIN);
        } else if (sourceType == Constants.SOURCE_TYPE_CUSTOM) {
            // 用户自建仅本人可见
            wrapper.eq(Book::getSourceType, Constants.SOURCE_TYPE_CUSTOM);
            if (currentUserId != null) {
                wrapper.eq(Book::getCreatorId, currentUserId);
            }
        }

        if (StringUtils.hasText(dto.getCategory())) {
            wrapper.eq(Book::getCategory, dto.getCategory());
        }
        if (StringUtils.hasText(dto.getDifficulty())) {
            wrapper.eq(Book::getDifficulty, mapDifficulty(dto.getDifficulty()));
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w
                    .like(Book::getTitle, dto.getKeyword())
                    .or().like(Book::getBookName, dto.getKeyword())
                    .or().like(Book::getAuthor, dto.getKeyword()));
        }

        wrapper.orderByDesc(Book::getCreatedAt);
        return bookMapper.selectPage(page, wrapper);
    }

    @Override
    public Book getBookById(Long id, Long currentUserId) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }
        // 自建素材仅本人可见
        if (book.getSourceType() == Constants.SOURCE_TYPE_CUSTOM) {
            if (currentUserId == null || !currentUserId.equals(book.getCreatorId())) {
                throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
            }
        }
        return book;
    }

    @Override
    public Page<Book> getMyBooks(Long userId, Integer page, Integer size) {
        Page<Book> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getCreatorId, userId)
                .eq(Book::getSourceType, Constants.SOURCE_TYPE_CUSTOM)
                .orderByDesc(Book::getCreatedAt);
        return bookMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Book createBook(BookCreateDTO dto, Long userId) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setBookName(dto.getBookName());
        book.setAuthor(dto.getAuthor());
        book.setCategory(dto.getCategory());
        book.setTags(dto.getTags());
        // content 仍然保存完整文本（向后兼容），同时也拆分到章节表
        book.setContent(dto.getContent());
        book.setDifficulty(mapDifficulty(dto.getDifficulty()));
        book.setWordCount(calculateWordCount(dto.getContent()));
        book.setSourceType(Constants.SOURCE_TYPE_CUSTOM);
        book.setCreatorId(userId);
        book.setStatus(1);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.insert(book);

        // 拆分章节并存储（前端已解析的优先，避免重复拆分损耗分隔线信息）
        if (dto.getChapters() != null && !dto.getChapters().isEmpty()) {
            saveChapters(book.getId(), dto.getChapters());
        } else if (StringUtils.hasText(dto.getContent())) {
            saveChapters(book.getId(), dto.getContent());
        }

        return book;
    }

    @Override
    public Book updateBook(Long id, BookCreateDTO dto, Long userId) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }
        if (!userId.equals(book.getCreatorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权修改他人素材");
        }

        if (dto.getTitle() != null) {
            book.setTitle(dto.getTitle());
        }
        if (dto.getBookName() != null) {
            book.setBookName(dto.getBookName());
        }
        if (dto.getAuthor() != null) {
            book.setAuthor(dto.getAuthor());
        }
        if (dto.getCategory() != null) {
            book.setCategory(dto.getCategory());
        }
        if (dto.getTags() != null) {
            book.setTags(dto.getTags());
        }
        if (dto.getContent() != null) {
            book.setContent(dto.getContent());
            book.setWordCount(calculateWordCount(dto.getContent()));
            // 内容变更后重建章节（前端已解析的优先）
            deleteChaptersByBookId(id);
            if (dto.getChapters() != null && !dto.getChapters().isEmpty()) {
                saveChapters(id, dto.getChapters());
            } else {
                saveChapters(id, dto.getContent());
            }
        }
        if (dto.getDifficulty() != null) {
            book.setDifficulty(mapDifficulty(dto.getDifficulty()));
        }
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
        return book;
    }

    @Override
    public void deleteBook(Long id, Long userId) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }
        if (!userId.equals(book.getCreatorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除他人素材");
        }
        chapterMapper.delete(new LambdaQueryWrapper<BookChapter>().eq(BookChapter::getBookId, id));
        bookMapper.deleteById(id);
    }

    @Override
    public FileParseResult parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String fileName = originalFilename != null ? originalFilename : "unknown.txt";

        // 只支持 TXT 文件
        if (!fileName.toLowerCase().endsWith(".txt")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 .txt 文件");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            String fullText = sb.toString();
            // 检测编码问题：如果是乱码，尝试 GBK
            if (looksLikeMojibake(fullText)) {
                // 重新用 GBK 读取
                try (BufferedReader gbkReader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), java.nio.charset.Charset.forName("GBK")))) {
                    sb = new StringBuilder();
                    while ((line = gbkReader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    fullText = sb.toString();
                }
            }

            List<ChapterItem> chapters = ChapterSplitUtil.split(fullText);
            int totalWords = fullText.replaceAll("\\s+", "").length();

            return new FileParseResult(fileName, totalWords, chapters);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件解析失败: " + e.getMessage());
        }
    }

    private String getPreview(String text, int maxLen) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cleaned = text.replaceAll("\\s+", "");
        if (cleaned.length() <= maxLen) {
            return cleaned;
        }
        return cleaned.substring(0, maxLen) + "...";
    }

    /** 简单检测是否乱码：UTF-8 下出现连续的乱码字符 */
    private boolean looksLikeMojibake(String text) {
        if (text == null || text.length() < 50) {
            return false;
        }
        // 取前200个字符检测：如果有大量不可打印字符或替代字符
        String sample = text.length() > 200 ? text.substring(0, 200) : text;
        int suspicious = 0;
        for (char c : sample.toCharArray()) {
            if (c == '\uFFFD' || (c >= 0x80 && c <= 0x9F)) {
                suspicious++;
            }
        }
        return suspicious > sample.length() / 5;
    }

    private int calculateWordCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return content.length();
    }

    /**
     * 将前端难度字符串映射为数据库 TINYINT
     * easy→1, medium→2, hard→3, 其他→2
     */
    private int mapDifficulty(String difficulty) {
        if (difficulty == null) return 2;
        switch (difficulty.toLowerCase()) {
            case "easy":   return 1;
            case "medium": return 2;
            case "hard":   return 3;
            default:       return 2;
        }
    }

    @Override
    public String getPracticeContent(Long bookId, Integer chapterIndex) {
        if (chapterIndex != null) {
            // 从章节表获取指定章节内容
            BookChapter chapter = chapterMapper.selectOne(
                    new LambdaQueryWrapper<BookChapter>()
                            .eq(BookChapter::getBookId, bookId)
                            .eq(BookChapter::getChapterIdx, chapterIndex)
                            .last("LIMIT 1"));
            if (chapter != null && chapter.getContent() != null) {
                return chapter.getContent();
            }
        }
        // 回退：返回全书内容
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }
        return book.getContent() != null ? book.getContent() : "";
    }

    @Override
    public List<ChapterItem> getChapters(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }

        // 优先从章节表查询
        List<BookChapter> dbChapters = chapterMapper.selectList(
            new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, bookId)
                .orderByAsc(BookChapter::getChapterIdx));

        if (!dbChapters.isEmpty()) {
            List<ChapterItem> result = new ArrayList<>();
            for (BookChapter ch : dbChapters) {
                ChapterItem item = new ChapterItem();
                item.setIndex(ch.getChapterIdx());
                item.setTitle(ch.getTitle());
                item.setContent(ch.getContent());
                item.setWordCount(ch.getWordCount());
                item.setPreview(getPreview(ch.getContent(), 100));
                result.add(item);
            }
            return result;
        }

        // 懒迁移：章节表为空，从 content 字段拆分并存储
        String content = book.getContent();
        if (!StringUtils.hasText(content)) {
            return new ArrayList<>();
        }
        log.info("[getChapters] lazy-migrate bookId={}, contentLength={}", bookId, content.length());
        List<ChapterItem> chapters = ChapterSplitUtil.split(content);
        saveChapters(bookId, chapters);
        log.info("[getChapters] lazy-migrated {} chapters for bookId={}", chapters.size(), bookId);
        return chapters;
    }

    // ═══════════════════════════════════════
    // 章节存储辅助方法
    // ═══════════════════════════════════════

    /**
     * 将原始文本拆分为章节并批量存储
     */
    private void saveChapters(Long bookId, String content) {
        List<ChapterItem> chapters = ChapterSplitUtil.split(content);
        saveChapters(bookId, chapters);
    }

    /**
     * 批量存储章节列表
     */
    private void saveChapters(Long bookId, List<ChapterItem> chapters) {
        List<BookChapter> entities = new ArrayList<>();
        for (ChapterItem item : chapters) {
            BookChapter ch = new BookChapter();
            ch.setBookId(bookId);
            ch.setChapterIdx(item.getIndex());
            ch.setTitle(item.getTitle());
            ch.setContent(item.getContent());
            ch.setWordCount(item.getWordCount());
            ch.setCreatedAt(LocalDateTime.now());
            entities.add(ch);
        }
        if (!entities.isEmpty()) {
            // 分批插入，每批最多 500 条，防止单条 SQL 超长
            int batchSize = 500;
            for (int i = 0; i < entities.size(); i += batchSize) {
                int end = Math.min(i + batchSize, entities.size());
                chapterMapper.batchInsert(entities.subList(i, end));
            }
        }
    }

    /**
     * 删除某本书的所有章节
     */
    private void deleteChaptersByBookId(Long bookId) {
        chapterMapper.delete(new LambdaQueryWrapper<BookChapter>().eq(BookChapter::getBookId, bookId));
    }
}
