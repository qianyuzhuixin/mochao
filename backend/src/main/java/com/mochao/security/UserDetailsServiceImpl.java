package com.mochao.security;

import com.mochao.common.constant.Constants;
import com.mochao.module.auth.entity.User;
import com.mochao.module.auth.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, usernameOrEmail)
                        .or()
                        .eq(User::getEmail, usernameOrEmail)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + usernameOrEmail);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用: " + usernameOrEmail);
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + (user.getRole() != null ? user.getRole() : Constants.ROLE_USER));

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
