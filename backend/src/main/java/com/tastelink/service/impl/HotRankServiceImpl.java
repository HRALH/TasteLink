package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tastelink.common.Constants;
import com.tastelink.entity.Review;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.service.HotRankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局点评审热度排行的 Redis 缓存实现。
 * <p>
 * 复合分 {@code score = like_count * LIKE_WEIGHT + min(reply_count, REPLY_CAP)}：点赞主导排序，
 * 评论数作为同点赞数下的次级权重（上限 REPLY_CAP 防止高评论数碾压点赞）。
 * <p>
 * 任何 Redis 异常都吞掉并记 warn——写侧靠对账修复漂移，读侧返回空让上层回退 MySQL，绝不把缓存故障抛给用户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotRankServiceImpl implements HotRankService {

    /** like_count 在复合分中的权重，使其远大于 reply_count（≤ REPLY_CAP）以确保点赞主导排序。 */
    private static final double LIKE_WEIGHT = 100.0;
    /** reply_count 对分数的影响上限，避免高评论数碾压点赞。 */
    private static final int REPLY_CAP = 99;

    private final StringRedisTemplate redis;
    private final ReviewMapper reviewMapper;

    @Value("${tastelink.rank.cache-enabled:true}")
    private boolean cacheEnabled;

    @Value("${tastelink.rank.zset-key:review:hot}")
    private String key;

    @Override
    public void onLike(Long reviewId) {
        if (!cacheEnabled || reviewId == null) {
            return;
        }
        try {
            redis.opsForZSet().incrementScore(key, String.valueOf(reviewId), LIKE_WEIGHT);
        } catch (Exception e) {
            log.warn("redis onLike failed, will be reconciled later: reviewId={}, err={}", reviewId, e.getMessage());
        }
    }

    @Override
    public void onUnlike(Long reviewId) {
        if (!cacheEnabled || reviewId == null) {
            return;
        }
        try {
            redis.opsForZSet().incrementScore(key, String.valueOf(reviewId), -LIKE_WEIGHT);
        } catch (Exception e) {
            log.warn("redis onUnlike failed, will be reconciled later: reviewId={}, err={}", reviewId, e.getMessage());
        }
    }

    @Override
    public List<Long> topReviewIds(int limit) {
        if (!cacheEnabled || limit <= 0) {
            return List.of();
        }
        try {
            var tuples = redis.opsForZSet().reverseRange(key, 0, limit - 1);
            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }
            List<Long> ids = new ArrayList<>(tuples.size());
            for (String member : tuples) {
                try {
                    ids.add(Long.valueOf(member));
                } catch (NumberFormatException ignore) {
                    // 跳过非法成员，靠对账清理
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("redis topReviewIds failed, fallback to MySQL: err={}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void rebuild(int topN) {
        if (!cacheEnabled || topN <= 0) {
            return;
        }
        // 以 MySQL 为准取 TopN 复合分；last() 拼接 raw SQL，topN 为 int 无注入风险
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getStatus, Constants.STATUS_NORMAL)
                .last("ORDER BY (like_count * 100 + LEAST(IFNULL(reply_count,0), " + REPLY_CAP + ")) DESC LIMIT " + topN);
        List<Review> top = reviewMapper.selectList(wrapper);
        try {
            // 快照式重建：先清空再写入，顺带清掉已软删/陈旧的成员。读路径遇空会回退 MySQL。
            redis.delete(key);
            for (Review r : top) {
                int like = r.getLikeCount() == null ? 0 : r.getLikeCount();
                int reply = r.getReplyCount() == null ? 0 : r.getReplyCount();
                double score = like * LIKE_WEIGHT + Math.min(reply, REPLY_CAP);
                redis.opsForZSet().add(key, String.valueOf(r.getId()), score);
            }
            log.info("hot-rank rebuild done: key={}, size={}", key, top.size());
        } catch (Exception e) {
            log.warn("redis rebuild write failed, will retry next cycle: err={}", e.getMessage());
        }
    }

    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
}
