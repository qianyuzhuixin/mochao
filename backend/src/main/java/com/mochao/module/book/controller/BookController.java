package com.mochao.module.book.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.book.dto.BookCreateDTO;
import com.mochao.module.book.dto.BookQueryDTO;
import com.mochao.module.book.dto.ChapterItem;
import com.mochao.module.book.dto.FileParseResult;
import com.mochao.module.book.entity.Book;
import com.mochao.module.book.service.BookService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public Result<Page<Book>> list(BookQueryDTO dto) {
        Long currentUserId = SecurityUtils.isAuthenticated() ? SecurityUtils.getCurrentUserId() : null;
        return Result.success(bookService.getBookList(dto, currentUserId));
    }

    @GetMapping("/{id}")
    public Result<Book> detail(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.isAuthenticated() ? SecurityUtils.getCurrentUserId() : null;
        return Result.success(bookService.getBookById(id, currentUserId));
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        List<String> categories = Arrays.asList(
                "玄幻", "都市", "武侠", "科幻", "悬疑", "历史", "言情", "仙侠", "奇幻");
        return Result.success(categories);
    }

    @GetMapping("/my")
    public Result<Page<Book>> myBooks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(bookService.getMyBooks(userId, page, size));
    }

    @PostMapping
    public Result<Book> create(@Valid @RequestBody BookCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(bookService.createBook(dto, userId));
    }

    @PutMapping("/{id}")
    public Result<Book> update(@PathVariable Long id, @RequestBody BookCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(bookService.updateBook(id, dto, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        bookService.deleteBook(id, userId);
        return Result.success();
    }

    @PostMapping("/parse-file")
    public Result<FileParseResult> parseFile(@RequestParam("file") MultipartFile file) {
        return Result.success(bookService.parseFile(file));
    }

    @GetMapping("/{id}/chapters")
    public Result<List<ChapterItem>> getChapters(@PathVariable Long id) {
        return Result.success(bookService.getChapters(id));
    }
}
