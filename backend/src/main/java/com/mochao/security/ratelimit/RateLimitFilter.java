package com.mochao.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * API 限流过滤器 — 基于 Bucket4j 8.x
 *
 * 限流策略（可通过 rate-limit.* 配置项调整）:
 * - /v1/ai/** → 每用户每分钟 aiCapacity 次
 * - /v1/auth/** → 每IP每分钟 authCapacity 次
 * - 其他 → 每用户每分钟 generalCapacity 次
 *
 * 超限返回 429 Too Many Requests + 中文提示 + Retry-After 头
 *
 * 当 rate-limit.enabled=false 时，此过滤器不做任何拦截
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;

    public RateLimitFilter(RateLimitService rateLimitService, RateLimitProperties properties) {
        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 限流关闭时直接放行
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        Long userId = extractUserId(request);
        String clientIp = extractClientIp(request);

        Bucket bucket;
        if (path.contains("/v1/ai/")) {
            bucket = userId != null ? rateLimitService.resolveAiBucket(userId)
                    : rateLimitService.resolveAuthBucket(clientIp);
        } else if (path.contains("/v1/auth/")) {
            bucket = rateLimitService.resolveAuthBucket(clientIp);
        } else {
            bucket = userId != null ? rateLimitService.resolveGeneralBucket(userId)
                    : rateLimitService.resolveAuthBucket(clientIp);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitTime = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Retry-After", String.valueOf(Math.max(waitTime, 1)));
            response.getWriter().write(
                    "{\"code\":429,\"message\":\"操作过于频繁，请稍后再试\",\"data\":null}");
        }
    }

    private Long extractUserId(HttpServletRequest request) {
        Object principal = request.getAttribute("userId");
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
