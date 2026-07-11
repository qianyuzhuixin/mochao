package com.mochao.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mochao.module.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
