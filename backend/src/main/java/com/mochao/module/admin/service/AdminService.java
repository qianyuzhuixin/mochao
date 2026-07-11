package com.mochao.module.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.module.auth.entity.User;
import com.mochao.module.book.dto.BookCreateDTO;
import com.mochao.module.book.entity.Book;

import java.util.List;
import java.util.Map;

public interface AdminService {

    Map<String, Object> getDashboard();

    Page<User> getUserList(Integer page, Integer size, String keyword);

    void updateUserStatus(Long userId, Integer status, Long adminId);

    Page<Book> getBookList(Integer page, Integer size, Integer sourceType);

    Book createBook(BookCreateDTO dto, Long adminId);

    Book updateBook(Long id, BookCreateDTO dto, Long adminId);

    void deleteBook(Long id, Long adminId);

    Integer importBooks(List<BookCreateDTO> books, Long adminId);
}
