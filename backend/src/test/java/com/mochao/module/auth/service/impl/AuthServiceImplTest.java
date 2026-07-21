package com.mochao.module.auth.service.impl;

import com.mochao.common.exception.BusinessException;
import com.mochao.common.result.ResultCode;
import com.mochao.module.auth.dto.*;
import com.mochao.module.auth.entity.User;
import com.mochao.module.auth.mapper.UserMapper;
import com.mochao.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 *
 * 编写指南 (给团队成员):
 *  1. 使用 @ExtendWith(MockitoExtension.class) 启用 Mockito
 *  2. @Mock 模拟外部依赖，@InjectMocks 注入被测试对象
 *  3. 测试命名: 方法名_场景_预期结果
 *  4. 结构: Given(准备Mock数据) -> When(调用被测方法) -> Then(验证结果)
 *  5. 验证业务异常: assertThrows(BusinessException.class, () -> ...)
 *  6. 验证Mock调用: verify(mock, times(n)).methodName(...)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    // ===== 注册测试 =====

    @Test
    @DisplayName("register_正常注册_返回token和用户信息")
    void register_正常注册_返回token和用户信息() {
        // Given
        RegisterDTO dto = buildRegisterDTO("newuser", "new@test.com", "password123");
        when(userMapper.selectCount(any())).thenReturn(0L); // 用户名和邮箱都不存在
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtTokenProvider.generateToken(anyLong(), eq("newuser"), eq("ROLE_USER")))
                .thenReturn("generated-jwt-token");

        // When
        LoginResponseDTO result = authService.register(dto);

        // Then
        assertNotNull(result);
        assertEquals("generated-jwt-token", result.getToken());
        assertEquals("newuser", result.getUserInfo().getUsername());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("register_用户名已存在_抛出BusinessException")
    void register_用户名已存在_抛出BusinessException() {
        // Given
        RegisterDTO dto = buildRegisterDTO("existing", "new@test.com", "password123");
        when(userMapper.selectCount(any())).thenReturn(1L); // 模拟用户名已存在

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(dto));
        assertEquals(ResultCode.USER_EXISTS, ex.getResultCode());
        assertTrue(ex.getMessage().contains("用户名已存在"));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("register_邮箱已注册_抛出BusinessException")
    void register_邮箱已注册_抛出BusinessException() {
        // Given
        RegisterDTO dto = buildRegisterDTO("newuser", "taken@test.com", "password123");
        // 第一次 selectCount（用户名检查）返回 0，第二次（邮箱检查）返回 1
        when(userMapper.selectCount(any())).thenReturn(0L).thenReturn(1L);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(dto));
        assertEquals(ResultCode.USER_EXISTS, ex.getResultCode());
        assertTrue(ex.getMessage().contains("邮箱已被注册"));
    }

    // ===== 登录测试 =====

    @Test
    @DisplayName("login_用户名密码正确_返回token")
    void login_用户名密码正确_返回token() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("correct-password");

        User user = buildUser(1L, "testuser", "ROLE_USER", 1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "testuser", "ROLE_USER"))
                .thenReturn("login-token");

        // When
        LoginResponseDTO result = authService.login(dto);

        // Then
        assertNotNull(result);
        assertEquals("login-token", result.getToken());
        assertEquals("testuser", result.getUserInfo().getUsername());
    }

    @Test
    @DisplayName("login_用户不存在_抛出BusinessException")
    void login_用户不存在_抛出BusinessException() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setUsername("nonexistent");
        dto.setPassword("any-password");
        when(userMapper.selectOne(any())).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ResultCode.USER_NOT_FOUND, ex.getResultCode());
    }

    @Test
    @DisplayName("login_密码错误_抛出BusinessException")
    void login_密码错误_抛出BusinessException() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrong-password");

        User user = buildUser(1L, "testuser", "ROLE_USER", 1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ResultCode.PASSWORD_ERROR, ex.getResultCode());
    }

    @Test
    @DisplayName("login_账号被禁用_抛出BusinessException")
    void login_账号被禁用_抛出BusinessException() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setUsername("disabled-user");
        dto.setPassword("correct-password");

        User user = buildUser(1L, "disabled-user", "ROLE_USER", 0); // status=0 表示禁用
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ResultCode.FORBIDDEN, ex.getResultCode());
        assertTrue(ex.getMessage().contains("已被禁用"));
    }

    @Test
    @DisplayName("login_使用邮箱登录_成功")
    void login_使用邮箱登录_成功() {
        // Given
        LoginDTO dto = new LoginDTO();
        dto.setUsername("user@test.com"); // 用邮箱作为 username 输入
        dto.setPassword("correct-password");

        User user = buildUser(2L, "user2", "ROLE_USER", 1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(2L, "user2", "ROLE_USER")).thenReturn("email-login-token");

        // When
        LoginResponseDTO result = authService.login(dto);

        // Then
        assertEquals("email-login-token", result.getToken());
        assertEquals("user2", result.getUserInfo().getUsername());
    }

    // ===== 修改密码测试 =====

    @Test
    @DisplayName("changePassword_旧密码正确_修改成功")
    void changePassword_旧密码正确_修改成功() {
        // Given — 注意: 此方法依赖 SecurityUtils.getCurrentUserId()（静态方法），
        // 完整测试需要 MockedStatic<SecurityUtils> 或使用 Spring 集成测试。
        // 这里演示的是纯单元测试的局限性，实际请参考集成测试模板。
    }

    // ===== 辅助方法 =====

    private RegisterDTO buildRegisterDTO(String username, String email, String password) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private User buildUser(Long id, String username, String role, int status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword("encoded-password");
        user.setNickname(username);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
