package com.tastelink.security;

import com.tastelink.config.RedisKeyNamespace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JwtBlacklistService 单测（B1-3，Mockito）。
 * 覆盖：revoke 写剩余 TTL / 过期不写入 / Redis 异常吞掉；isRevoked 命中与 fail-open。
 */
@ExtendWith(MockitoExtension.class)
class JwtBlacklistServiceTest {

    private static final String KEY = "tastelink:local:jwt:blacklist:jti-1";

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private RedisKeyNamespace redisKeys;
    @Mock
    private ValueOperations<String, String> valueOps;

    private JwtBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new JwtBlacklistService(redis, redisKeys);
        org.mockito.Mockito.lenient().when(redisKeys.key(anyString())).thenAnswer(inv -> "tastelink:local:" + inv.getArgument(0));
    }

    @Test
    void revoke_writesKeyWithRemainingTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);

        service.revoke("jti-1", 3600);

        verify(valueOps).set(eq(KEY), eq("1"), eq(Duration.ofSeconds(3600)));
    }

    @Test
    void revoke_nonPositiveTtl_skipsWrite() {
        service.revoke("jti-1", 0);
        service.revoke("jti-1", -5);
        service.revoke(null, 100);

        verifyNoInteractionsExceptNamespace();
    }

    @Test
    void revoke_redisError_swallowed() {
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> service.revoke("jti-1", 60));
    }

    @Test
    void isRevoked_hit_returnsTrue() {
        when(redis.hasKey(KEY)).thenReturn(true);
        assertTrue(service.isRevoked("jti-1"));
    }

    @Test
    void isRevoked_missOrBlank_returnsFalse() {
        when(redis.hasKey(KEY)).thenReturn(false);
        assertFalse(service.isRevoked("jti-1"));
        assertFalse(service.isRevoked(null));
        assertFalse(service.isRevoked(""));
    }

    @Test
    void isRevoked_redisError_failOpen() {
        when(redis.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));
        assertFalse(service.isRevoked("jti-1"));
    }

    private void verifyNoInteractionsExceptNamespace() {
        verify(redis, never()).opsForValue();
        verify(redis, never()).hasKey(anyString());
    }
}
