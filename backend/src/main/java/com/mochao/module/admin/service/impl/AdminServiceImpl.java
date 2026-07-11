package com.mochao.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.constant.Constants;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.admin.entity.AdminLog;
import com.mochao.module.admin.mapper.AdminLogMapper;
import com.mochao.module.admin.service.AdminService;
import com.mochao.module.auth.entity.User;
import com.mochao.module.auth.mapper.UserMapper;
import com.mochao.module.book.dto.BookCreateDTO;
import com.mochao.module.book.dto.ChapterItem;
import com.mochao.module.book.entity.Book;
import com.mochao.module.book.entity.BookChapter;
import com.mochao.module.book.mapper.BookChapterMapper;
import com.mochao.module.book.mapper.BookMapper;
import com.mochao.module.book.util.ChapterSplitUtil;
import com.mochao.module.novel.entity.Novel;
import com.mochao.module.novel.mapper.NovelMapper;
import com.mochao.module.practice.entity.PracticeSession;
import com.mochao.module.practice.mapper.PracticeSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final BookChapterMapper chapterMapper;
    private final NovelMapper novelMapper;
    private final PracticeSessionMapper practiceSessionMapper;
    private final AdminLogMapper adminLogMapper;

    public AdminServiceImpl(UserMapper userMapper,
                            BookMapper bookMapper,
                            BookChapterMapper chapterMapper,
                            NovelMapper novelMapper,
                            PracticeSessionMapper practiceSessionMapper,
                            AdminLogMapper adminLogMapper) {
        this.userMapper = userMapper;
        this.bookMapper = bookMapper;
        this.chapterMapper = chapterMapper;
        this.novelMapper = novelMapper;
        this.practiceSessionMapper = practiceSessionMapper;
        this.adminLogMapper = adminLogMapper;
    }

    @Override
    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // 用户统计
        long totalUsers = userMapper.selectCount(null);
        long activeUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, Constants.STATUS_ACTIVE));
        dashboard.put("totalUsers", totalUsers);
        dashboard.put("activeUsers", activeUsers);

        // 素材统计
        long totalBooks = bookMapper.selectCount(null);
        long builtinBooks = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getSourceType, Constants.SOURCE_TYPE_BUILTIN));
        long customBooks = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getSourceType, Constants.SOURCE_TYPE_CUSTOM));
        dashboard.put("totalBooks", totalBooks);
        dashboard.put("builtinBooks", builtinBooks);
        dashboard.put("customBooks", customBooks);

        // 小说统计
        long totalNovels = novelMapper.selectCount(null);
        long ongoingNovels = novelMapper.selectCount(
                new LambdaQueryWrapper<Novel>().eq(Novel::getStatus, Constants.NOVEL_STATUS_ONGOING));
        long completedNovels = novelMapper.selectCount(
                new LambdaQueryWrapper<Novel>().eq(Novel::getStatus, Constants.NOVEL_STATUS_COMPLETED));
        dashboard.put("totalNovels", totalNovels);
        dashboard.put("ongoingNovels", ongoingNovels);
        dashboard.put("completedNovels", completedNovels);

        // 练习统计
        long totalPractices = practiceSessionMapper.selectCount(null);
        long completedPractices = practiceSessionMapper.selectCount(
                new LambdaQueryWrapper<PracticeSession>().eq(PracticeSession::getStatus, Constants.PRACTICE_STATUS_COMPLETED));
        dashboard.put("totalPractices", totalPractices);
        dashboard.put("completedPractices", completedPractices);

        return dashboard;
    }

    @Override
    public Page<User> getUserList(Integer page, Integer size, String keyword) {
        Page<User> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status, Long adminId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        saveAdminLog(adminId, "update_status", "user", userId,
                "修改用户状态: " + userId + " -> " + status);
    }

    @Override
    public Page<Book> getBookList(Integer page, Integer size, Integer sourceType) {
        Page<Book> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (sourceType != null) {
            wrapper.eq(Book::getSourceType, sourceType);
        }
        wrapper.orderByDesc(Book::getCreatedAt);
        return bookMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Book createBook(BookCreateDTO dto, Long adminId) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setBookName(dto.getBookName());
        book.setAuthor(dto.getAuthor());
        book.setCategory(dto.getCategory());
        book.setTags(dto.getTags());
        book.setContent(dto.getContent());
        book.setDifficulty(mapDifficulty(dto.getDifficulty()));
        book.setWordCount(dto.getContent() != null ? dto.getContent().length() : 0);
        book.setSourceType(Constants.SOURCE_TYPE_BUILTIN);
        book.setCreatorId(adminId);
        book.setStatus(1);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.insert(book);

        // 同步章节存储
        if (StringUtils.hasText(dto.getContent())) {
            saveBookChapters(book.getId(), dto.getContent());
        }

        saveAdminLog(adminId, "create_book", "book", book.getId(), "创建内置素材: " + book.getTitle());
        return book;
    }

    @Override
    public Book updateBook(Long id, BookCreateDTO dto, Long adminId) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }
        if (dto.getTitle() != null) book.setTitle(dto.getTitle());
        if (dto.getBookName() != null) book.setBookName(dto.getBookName());
        if (dto.getAuthor() != null) book.setAuthor(dto.getAuthor());
        if (dto.getCategory() != null) book.setCategory(dto.getCategory());
        if (dto.getTags() != null) book.setTags(dto.getTags());
        if (dto.getContent() != null) {
            book.setContent(dto.getContent());
            book.setWordCount(dto.getContent().length());
            // 内容变更后重建章节
            chapterMapper.delete(new LambdaQueryWrapper<BookChapter>().eq(BookChapter::getBookId, id));
            saveBookChapters(id, dto.getContent());
        }
        if (dto.getDifficulty() != null) book.setDifficulty(mapDifficulty(dto.getDifficulty()));
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);

        saveAdminLog(adminId, "update_book", "book", id, "更新内置素材: " + id);
        return book;
    }

    @Override
    public void deleteBook(Long id, Long adminId) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "素材不存在");
        }
        chapterMapper.delete(new LambdaQueryWrapper<BookChapter>().eq(BookChapter::getBookId, id));
        bookMapper.deleteById(id);
        saveAdminLog(adminId, "delete_book", "book", id, "删除素材: " + id);
    }

    @Override
    public Integer importBooks(List<BookCreateDTO> books, Long adminId) {
        int count = 0;
        for (BookCreateDTO dto : books) {
            Book book = new Book();
            book.setTitle(dto.getTitle());
            book.setBookName(dto.getBookName());
            book.setAuthor(dto.getAuthor());
            book.setCategory(dto.getCategory());
            book.setTags(dto.getTags());
            book.setContent(dto.getContent());
            book.setDifficulty(mapDifficulty(dto.getDifficulty()));
            book.setWordCount(dto.getContent() != null ? dto.getContent().length() : 0);
            book.setSourceType(Constants.SOURCE_TYPE_BUILTIN);
            book.setCreatorId(adminId);
            book.setStatus(1);
            book.setCreatedAt(LocalDateTime.now());
            book.setUpdatedAt(LocalDateTime.now());
            bookMapper.insert(book);
            // 同步章节存储
            if (StringUtils.hasText(dto.getContent())) {
                saveBookChapters(book.getId(), dto.getContent());
            }
            count++;
        }
        saveAdminLog(adminId, "import_books", "book", null, "批量导入素材: " + count + "条");
        return count;
    }

    private void saveAdminLog(Long adminId, String action, String targetType, Long targetId, String detail) {
        AdminLog log = new AdminLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        adminLogMapper.insert(log);
    }

    /**
     * 将书籍内容拆分为章节并存储到 t_book_chapter
     */
    private void saveBookChapters(Long bookId, String content) {
        List<ChapterItem> chapters = ChapterSplitUtil.split(content);
        for (ChapterItem item : chapters) {
            BookChapter ch = new BookChapter();
            ch.setBookId(bookId);
            ch.setChapterIdx(item.getIndex());
            ch.setTitle(item.getTitle());
            ch.setContent(item.getContent());
            ch.setWordCount(item.getWordCount());
            ch.setCreatedAt(LocalDateTime.now());
            chapterMapper.insert(ch);
        }
    }

    private Integer mapDifficulty(String d) {
        if (d == null) return 2;
        switch (d) {
            case "easy": return 1;
            case "hard": return 3;
            default: return 2;
        }
    }
}
