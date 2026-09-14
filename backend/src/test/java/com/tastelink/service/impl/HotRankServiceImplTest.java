package com.tastelink.service.impl;

import com.tastelink.config.RedisKeyNamespace;
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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HotRankServiceImpl 逻辑单测（Mockito，不依赖 Docker，随 {@code mvn test} 运行）。
 * 仅验证与 Redis/MySQL 的交互语义；真 Redis 端到端见 {@link HotRankServiceIT}。
 */
@ExtendWith(MockitoExtension.class)
class HotRankServiceImplTest {

    private static final String LIVE_KEY = "tastelink:local:review:hot";

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private RedisKeyNamespace redisKeys;
    @Mock
    private ZSetOperations<String, String> zSetOps;

    private HotRankServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HotRankServiceImpl(redis, reviewMapper, redisKeys);
        ReflectionTestUtils.setField(service, "cacheEnabled", true);
        ReflectionTestUtils.setField(service, "zsetSuffix", "review:hot");
        org.mockito.Mockito.lenient().when(redisKeys.key(anyString()))
                .thenAnswer(inv -> "tastelink:local:" + inv.getArgument(0));
    }

    @Test
    void onLike_incrementsByWeight_onEnvironmentPrefixedKey() {
        when(redis.opsForZSet()).thenReturn(zSetOps);
        service.onLike(10L);
        verify(zSetOps).incrementScore(eq(LIVE_KEY), eq("10"), eq(100.0d));
    }

    @Test
    void onUnlike_decrementsByWeight_onEnvironmentPrefixedKey() {
        when(redis.opsForZSet()).thenReturn(zSetOps);
        service.onUnlike(10L);
        verify(zSetOps).incrementScore(eq(LIVE_KEY), eq("10"), eq(-100.0d));
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
        when(zSetOps.reverseRange(eq(LIVE_KEY), eq(0L), eq(9L)))
                .thenReturn(new LinkedHashSet<>(List.of("3", "1", "2")));
        assertEquals(List.of(3L, 1L, 2L), service.topReviewIds(10));
    }

    @Test
    void topReviewIds_redisError_returnsEmpty() {
        when(redis.opsForZSet()).thenThrow(new RuntimeException("redis down"));
        assertTrue(service.topReviewIds(10).isEmpty());
    }

    // ---------- B4-4 原子化重建 ----------

    @Test
    void rebuild_writesTmpKeyThenRenames_noReadWindow() {
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

        // 临时 key：liveKey + ":rebuild-<nano>"；批量 TypedTuple 写入；原子 RENAME 到 live key
        verify(zSetOps).add(anyString(), anySet());
        verify(redis).rename(anyString(), eq(LIVE_KEY));
        // 不再先 delete live key（消除读空窗）
        verify(redis, never()).delete(eq(LIVE_KEY));
    }

    @Test
    void rebuild_scoresAreLikeWeightedPlusReplyCapped() {
        Review r1 = new Review();
        r1.setId(1L);
        r1.setLikeCount(5);
        r1.setReplyCount(2);   // score=502
        Review r2 = new Review();
        r2.setId(2L);
        r2.setLikeCount(1);
        r2.setReplyCount(3);   // score=103
        when(reviewMapper.selectList(any())).thenReturn(List.of(r1, r2));
        when(redis.opsForZSet()).thenReturn(zSetOps);

        service.rebuild(100);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Set<ZSetOperations.TypedTuple<String>>> cap =
                org.mockito.ArgumentCaptor.forClass(Set.class);
        verify(zSetOps).add(anyString(), cap.capture());
        Map<String, Double> scores = new java.util.HashMap<>();
        for (ZSetOperations.TypedTuple<String> t : cap.getValue()) {
            scores.put(t.getValue(), t.getScore());
        }
        assertEquals(502.0d, scores.get("1"));
        assertEquals(103.0d, scores.get("2"));
    }

    @Test
    void rebuild_emptyTop_stillRenamesToEmptyKeyToClearStale() {
        // 无评分数据时仍 RENAME 一个空临时 key 覆盖 live key，覆盖掉陈旧成员
        when(reviewMapper.selectList(any())).thenReturn(List.of());

        service.rebuild(100);

        verify(redis).rename(anyString(), eq(LIVE_KEY));
        verify(zSetOps, never()).add(anyString(), anySet());
    }
}
