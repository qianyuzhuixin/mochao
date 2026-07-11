package com.mochao.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.common.result.Result;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.admin.service.AdminService;
import com.mochao.module.auth.entity.User;
import com.mochao.module.book.dto.BookCreateDTO;
import com.mochao.module.book.entity.Book;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(adminService.getDashboard());
    }

    @GetMapping("/users")
    public Result<Page<User>> users(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getUserList(page, size, keyword));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                          @RequestParam Integer status) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.updateUserStatus(id, status, adminId);
        return Result.success();
    }

    @GetMapping("/books")
    public Result<Page<Book>> bookList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer sourceType) {
        return Result.success(adminService.getBookList(page, size, sourceType));
    }

    @PostMapping("/books")
    public Result<Book> createBook(@Valid @RequestBody BookCreateDTO dto) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return Result.success(adminService.createBook(dto, adminId));
    }

    @PutMapping("/books/{id}")
    public Result<Book> updateBook(@PathVariable Long id, @RequestBody BookCreateDTO dto) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return Result.success(adminService.updateBook(id, dto, adminId));
    }

    @DeleteMapping("/books/{id}")
    public Result<Void> deleteBook(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.deleteBook(id, adminId);
        return Result.success();
    }

    @PostMapping("/books/import")
    public Result<Integer> importBooks(@RequestBody List<BookCreateDTO> books) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return Result.success(adminService.importBooks(books, adminId));
    }
}
