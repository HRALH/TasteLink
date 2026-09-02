package com.tastelink.service.impl;

import com.tastelink.entity.Review;
import com.tastelink.mapper.ReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HotRankServiceImpl 逻辑单测（Mockito，不依赖 Docker，随 {@code mvn test} 运行）。
 * 仅验证与 Redis/MySQL 的交互语义；真 Redis 端到端见 {@link HotRankServiceIT}。
 */
@ExtendWith(MockitoExtension.class)
class HotRankServiceImplTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    private HotRankServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HotRankServiceImpl(redis, reviewMapper);
        ReflectionTestUtils.setField(service, "cacheEnabled", true);
        ReflectionTestUtils.setField(service, "key", "review:hot");
    }

    @Test
    void onLike_incrementsByWeight() {
        when(redis.opsForZSet()).thenReturn(zSetOps);
        service.onLike(10L);
        verify(zSetOps).incrementScore(eq("review:hot"), eq("10"), eq(100.0d));
    }

    @Test
    void onUnlike_decrementsByWeight() {
        when(redis.opsForZSet()).thenReturn(zSetOps);
        service.onUnlike(10L);
        verify(zSetOps).incrementScore(eq("review:hot"), eq("10"), eq(-100.0d));
    }

    @Test
    void onLike_disabled_isNoOp() {
        ReflectionTestUtils.setField(service, "cacheEnabled", false);
        service.onLike(10L);
        verifyNoInteractions(redis);
    }

    @Test
    void topReviewIds_preservesRankingOrder() {
        when(redis.opsForZSet()).thenReturn(zSetOps);
        // reverseRange 返回排行顺序；用 LinkedHashSet 模拟该顺序
        when(zSetOps.reverseRange(eq("review:hot"), eq(0L), eq(9L)))
                .thenReturn(new LinkedHashSet<>(List.of("3", "1", "2")));
        assertEquals(List.of(3L, 1L, 2L), service.topReviewIds(10));
    }

    @Test
    void topReviewIds_redisError_returnsEmpty() {
        when(redis.opsForZSet()).thenThrow(new RuntimeException("redis down"));
        assertTrue(service.topReviewIds(10).isEmpty());
    }

    @Test
    void rebuild_overwritesKeyWithDbSnapshot() {
        Review r1 = new Review();
        r1.setId(1L);
        r1.setLikeCount(5);
        r1.setReplyCount(2);
        Review r2 = new Review();
        r2.setId(2L);
        r2.setLikeCount(1);
        r2.setReplyCount(3);
        when(reviewMapper.selectList(any())).thenReturn(List.of(r1, r2));
        when(redis.opsForZSet()).thenReturn(zSetOps);

        service.rebuild(100);

        verify(redis).delete("review:hot");
        // score = like*100 + min(reply,99)：r1=502，r2=103
        verify(zSetOps).add(eq("review:hot"), eq("1"), eq(502.0d));
        verify(zSetOps).add(eq("review:hot"), eq("2"), eq(103.0d));
    }
}
