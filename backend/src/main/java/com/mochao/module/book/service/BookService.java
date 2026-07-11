package com.mochao.module.book.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.module.book.dto.BookCreateDTO;
import com.mochao.module.book.dto.BookQueryDTO;
import com.mochao.module.book.dto.ChapterItem;
import com.mochao.module.book.dto.FileParseResult;
import com.mochao.module.book.entity.Book;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {

    Page<Book> getBookList(BookQueryDTO dto, Long currentUserId);

    Book getBookById(Long id, Long currentUserId);

    Page<Book> getMyBooks(Long userId, Integer page, Integer size);

    Book createBook(BookCreateDTO dto, Long userId);

    Book updateBook(Long id, BookCreateDTO dto, Long userId);

    void deleteBook(Long id, Long userId);

    /** 解析上传的 TXT 文件，按章节拆分 */
    FileParseResult parseFile(MultipartFile file);

    /** 从已存储的书籍内容中解析章节列表 */
    List<ChapterItem> getChapters(Long bookId);
}
