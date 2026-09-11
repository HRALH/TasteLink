package com.tastelink.security;

import com.tastelink.config.RedisKeyNamespace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * JWT 吊销黑名单（B1-3）：登出/封号/改密后把 token 的 jti 写入黑名单（剩余 TTL），
 * {@link JwtAuthFilter} 命中即按未登录处理（401）。
 * <p>
 * Redis 缺席/异常时：revoke 吞掉（登出降级为仅前端清态），isRevoked fail-open 视为未吊销 + warn——
 * 与 Phase A/C 的「缓存故障不抛给用户」同范式；黑名单可用性靠 Redis 监控保障。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final StringRedisTemplate redis;
    private final RedisKeyNamespace redisKeys;

    /** 吊销 token：写入黑名单 key，TTL 为 token 剩余有效期；过期/无 jti 的旧 token 无可吊销直接跳过。 */
    public void revoke(String jti, long ttlSeconds) {
        if (!StringUtils.hasText(jti) || ttlSeconds <= 0) {
            return;
        }
        try {
            redis.opsForValue().set(redisKeys.key("jwt:blacklist:" + jti), "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("jwt blacklist revoke skipped (logout degrades to client-side): err={}", e.getMessage());
        }
    }

    /** 黑名单命中检查；Redis 异常 fail-open（视为未吊销）+ warn。 */
    public boolean isRevoked(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(redisKeys.key("jwt:blacklist:" + jti)));
        } catch (Exception e) {
            log.warn("jwt blacklist check failed, treat token as valid: err={}", e.getMessage());
            return false;
        }
    }
}
