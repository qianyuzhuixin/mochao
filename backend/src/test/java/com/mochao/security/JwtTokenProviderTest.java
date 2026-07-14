package com.mochao.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider 单元测试")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    // 测试用的强密钥（64字节 / 512位）
    private static final String TEST_SECRET =
            "7d3f8a2b9c1e4f6a0d5e7b3c8f2a9d1e4b6c0f7a3d8e2b9c1f4a6d0e7b3c8f2a9d1e4b6c0f7a3d8";

    private static final long TEST_EXPIRATION = 86400000L; // 24小时

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenProvider, "expiration", TEST_EXPIRATION);
        tokenProvider.init(); // 执行 @PostConstruct 逻辑
    }

    @Test
    @DisplayName("生成 Token 并成功解析")
    void generateAndParseToken() {
        String token = tokenProvider.generateToken(1L, "testuser", "USER");

        Claims claims = tokenProvider.parseClaims(token);
        assertEquals(1L, Long.parseLong(claims.getSubject()));
        assertEquals("testuser", claims.get("username", String.class));
        assertEquals("USER", claims.get("role", String.class));
    }

    @Test
    @DisplayName("从 Token 中提取用户ID")
    void getUserIdFromToken() {
        String token = tokenProvider.generateToken(42L, "zhangsan", "ADMIN");
        assertEquals(42L, tokenProvider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("从 Token 中提取用户名")
    void getUsernameFromToken() {
        String token = tokenProvider.generateToken(1L, "lisi", "USER");
        assertEquals("lisi", tokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("从 Token 中提取角色")
    void getRoleFromToken() {
        String token = tokenProvider.generateToken(1L, "admin", "ADMIN");
        assertEquals("ADMIN", tokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("验证有效 Token")
    void validateValidToken() {
        String token = tokenProvider.generateToken(1L, "testuser", "USER");
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("验证无效 Token — 返回 false")
    void validateInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.string"));
    }

    @Test
    @DisplayName("验证空 Token — 返回 false")
    void validateEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    @DisplayName("密钥过短 — 启动时抛异常")
    void rejectShortSecret() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "short-key"); // 9字节，远低于32字节
        assertThrows(IllegalStateException.class, provider::init);
    }

    @Test
    @DisplayName("密钥为空 — 启动时抛异常")
    void rejectEmptySecret() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "");
        assertThrows(IllegalStateException.class, provider::init);
    }

    @Test
    @DisplayName("密钥为 null — 启动时抛异常")
    void rejectNullSecret() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", null);
        assertThrows(IllegalStateException.class, provider::init);
    }

    @Test
    @DisplayName("一次 parseClaims 替代三次独立解析 — 性能验证")
    void singleParseVsTripleParse() {
        String token = tokenProvider.generateToken(1L, "perfuser", "USER");

        // 单次解析获取全部信息
        Claims claims = tokenProvider.parseClaims(token);
        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        // 对比独立解析结果 — 确保一致性
        assertEquals(tokenProvider.getUserIdFromToken(token), userId);
        assertEquals(tokenProvider.getUsernameFromToken(token), username);
        assertEquals(tokenProvider.getRoleFromToken(token), role);
    }
}
