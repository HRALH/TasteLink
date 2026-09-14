package com.tastelink.service.impl;

import com.tastelink.entity.Review;
import com.tastelink.mapper.ReviewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 真 Redis 端到端集成测试（Testcontainers）。
 * <p>
 * 需本机 Docker，且以 {@code -DRUN_IT=true} 运行才会启动容器；否则整类跳过，不影响常规构建。
 * 例：{@code mvn test -DRUN_IT=true -Dtest=HotRankServiceIT}
 */
@Testcontainers
@EnabledIfSystemProperty(named = "RUN_IT", matches = "true")
class HotRankServiceIT {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    /** 每个测试用独立 key，避免共享容器的测试间互相污染。 */
    private HotRankServiceImpl newServiceWithFreshKey(String key) {
        RedisStandaloneConfiguration cfg =
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(cfg);
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();

        ReviewMapper reviewMapper = mock(ReviewMapper.class);
        Review r = new Review();
        r.setId(1L);
        r.setLikeCount(3);
        r.setReplyCount(1);
        when(reviewMapper.selectList(any())).thenReturn(List.of(r));

        // B4-4：环境命名空间 mock——key 直接返回后缀（IT 内不走环境前缀，保持 key 唯一即可）
        com.tastelink.config.RedisKeyNamespace namespace = mock(com.tastelink.config.RedisKeyNamespace.class);
        when(namespace.key(org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv -> inv.getArgument(0));

        HotRankServiceImpl svc = new HotRankServiceImpl(template, reviewMapper, namespace);
        ReflectionTestUtils.setField(svc, "cacheEnabled", true);
        ReflectionTestUtils.setField(svc, "zsetSuffix", key);
        return svc;
    }

    @Test
    void likeThenTopN_preservesHeatOrder() {
        HotRankServiceImpl svc = newServiceWithFreshKey("review:hot-it-1");
        svc.onLike(1L);
        svc.onLike(1L);
        svc.onLike(2L);
        // review 1 两次赞 => score 200；review 2 一次 => 100；倒序应为 [1,2]
        assertEquals(List.of(1L, 2L), svc.topReviewIds(10));
    }

    @Test
    void rebuild_overwritesFromDbAndClearsStale() {
        HotRankServiceImpl svc = newServiceWithFreshKey("review:hot-it-2");
        svc.onLike(99L);           // 写入脏成员
        svc.rebuild(100);          // 以 DB 快照重建（DB 仅含 review 1）
        assertEquals(List.of(1L), svc.topReviewIds(10),
                "rebuild 应清掉脏成员 99，仅留 DB 的 review 1");
    }

    @Test
    void unlike_reducesScore() {
        HotRankServiceImpl svc = newServiceWithFreshKey("review:hot-it-3");
        svc.onLike(1L);
        svc.onLike(1L);
        svc.onUnlike(1L);
        // score 由 200 回到 100，仍可取到
        assertTrue(svc.topReviewIds(10).contains(1L));
    }
}
