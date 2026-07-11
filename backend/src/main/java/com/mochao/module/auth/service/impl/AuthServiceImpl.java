package com.mochao.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochao.common.constant.Constants;
import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.common.utils.SecurityUtils;
import com.mochao.module.auth.dto.*;
import com.mochao.module.auth.entity.User;
import com.mochao.module.auth.mapper.UserMapper;
import com.mochao.module.auth.service.AuthService;
import com.mochao.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public LoginResponseDTO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (usernameCount > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "用户名已存在");
        }

        // 检查邮箱是否已存在
        Long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (emailCount > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getUsername());
        user.setPreferredTheme("light");
        user.setFontSize(16);
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.STATUS_ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return buildLoginResponse(token, user);
    }

    @Override
    public LoginResponseDTO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .or()
                        .eq(User::getEmail, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return buildLoginResponse(token, user);
    }

    @Override
    public void logout() {
        // JWT无状态，前端清除token即可
        // 如需服务端失效，可将token加入Redis黑名单
    }

    @Override
    public LoginResponseDTO refreshToken() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return buildLoginResponse(token, user);
    }

    @Override
    public UserInfoDTO getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToUserInfoDTO(user);
    }

    @Override
    public UserInfoDTO updateProfile(UpdateProfileDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getSignature() != null) {
            user.setSignature(dto.getSignature());
        }
        if (dto.getPreferredTheme() != null) {
            user.setPreferredTheme(dto.getPreferredTheme());
        }
        if (dto.getFontSize() != null) {
            user.setFontSize(dto.getFontSize());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return convertToUserInfoDTO(user);
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "旧密码不正确");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private LoginResponseDTO buildLoginResponse(String token, User user) {
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUserInfo(convertToUserInfoDTO(user));
        return response;
    }

    private UserInfoDTO convertToUserInfoDTO(User user) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setSignature(user.getSignature());
        dto.setPreferredTheme(user.getPreferredTheme());
        dto.setFontSize(user.getFontSize());
        dto.setRole(user.getRole());
        return dto;
    }
}
