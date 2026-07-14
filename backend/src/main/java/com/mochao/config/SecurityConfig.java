package com.mochao.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochao.common.result.Result;
import com.mochao.common.result.ResultCode;
import com.mochao.security.JwtAuthenticationFilter;
import com.mochao.security.ratelimit.RateLimitFilter;
import com.mochao.security.ratelimit.RateLimitProperties;
import com.mochao.security.ratelimit.RateLimitService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         RateLimitService rateLimitService,
                         RateLimitProperties rateLimitProperties,
                         ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitService = rateLimitService;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // JWT 无状态认证，不使用 Cookie 传递凭据，因此禁用 CSRF 是安全的
            .csrf().disable()
            .cors()
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/v1/auth/**").permitAll()
                .antMatchers("/swagger-ui/**").permitAll()
                .antMatchers("/v3/api-docs/**").permitAll()
                // 音乐文件流式代理公开访问（Audio 元素无法携带 Auth Header）
                .antMatchers("/v1/files/music/**").permitAll()
                // 管理后台接口仅管理员可访问
                .antMatchers("/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                // 未认证访问受保护资源 → 返回 401（前端据此跳转登录）
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(objectMapper.writeValueAsString(
                            Result.error(ResultCode.UNAUTHORIZED, "请先登录")
                    ));
                })
                // 已认证但无权限（如非管理员访问 /v1/admin/**）→ 返回 403（前端只提示，不跳转）
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(objectMapper.writeValueAsString(
                            Result.error(ResultCode.FORBIDDEN, "没有权限访问")
                    ));
                });

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // 🔧 限流过滤器 — 在 JWT 认证之后执行，基于 userId/IP 限流
        // 当 rate-limit.enabled=false 时自动放行，不影响开发调试
        http.addFilterAfter(new RateLimitFilter(rateLimitService, rateLimitProperties), JwtAuthenticationFilter.class);

        return http.build();
    }
}
