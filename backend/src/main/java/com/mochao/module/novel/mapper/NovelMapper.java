package com.mochao.module.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mochao.module.novel.entity.Novel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelMapper extends BaseMapper<Novel> {
}
