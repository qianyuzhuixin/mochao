package com.mochao.security.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Bucket4j 8.x 的内存限流器
 *
 * 策略（可通过 rate-limit.* 配置项调整）:
 * - AI 相关接口: 每用户每分钟 N 次（默认 10，AI 调用成本高）
 * - 认证接口: 每IP每分钟 N 次（默认 20，防暴力破解）
 * - 通用接口: 每用户每分钟 N 次（默认 60，防高频刷接口）
 *
 * 注意: 当前使用内存存储，单实例限流。
 * 如果需要多实例分布式限流，请切换到 Bucket4j + Redis 方案。
 */
@Component
public class RateLimitService {

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    /** AI 接口限流: 每用户每分钟 aiCapacity 次 */
    public Bucket resolveAiBucket(Long userId) {
        return buckets.computeIfAbsent("ai:" + userId, key -> buildBucket(properties.getAiCapacity()));
    }

    /** 认证接口限流: 每IP每分钟 authCapacity 次 */
    public Bucket resolveAuthBucket(String clientIp) {
        return buckets.computeIfAbsent("auth:" + clientIp, key -> buildBucket(properties.getAuthCapacity()));
    }

    /** 通用接口限流: 每用户每分钟 generalCapacity 次 */
    public Bucket resolveGeneralBucket(Long userId) {
        return buckets.computeIfAbsent("general:" + userId, key -> buildBucket(properties.getGeneralCapacity()));
    }

    private Bucket buildBucket(int capacity) {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(capacity)
                        .refillGreedy(capacity, Duration.ofMinutes(1))
                )
                .build();
    }
}
