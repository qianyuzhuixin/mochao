package com.mochao.module.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mochao.module.book.entity.BookChapter;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BookChapterMapper extends BaseMapper<BookChapter> {

    /** 批量插入章节 */
    int batchInsert(List<BookChapter> chapters);
}
