package com.tastelink.service;

import java.util.List;

/**
 * 点评审热度排行缓存（v2 Phase A）：以 Redis SortedSet 维护全局热度，替代首页热门点评的 MySQL filesort。
 * <p>
 * 一致性口径：点赞/取消点赞在 DB 事务提交后写入 Redis（afterCommit），即便偶发漂移也由
 * {@link #rebuild(int)} 定时对账修复。读路径未命中/禁用/异常时回退 MySQL。
 */
public interface HotRankService {

    /** 点赞成功后调用：全局热度 +1 个权重单位（应于 DB 事务提交后调用）。 */
    void onLike(Long reviewId);

    /** 取消点赞成功后调用：全局热度 -1 个权重单位（应于 DB 事务提交后调用）。 */
    void onUnlike(Long reviewId);

    /**
     * 取热门点评 id 列表（按热度倒序）。缓存未命中、禁用或访问异常时返回空列表，
     * 调用方据此回退 MySQL 查询。
     */
    List<Long> topReviewIds(int limit);

    /**
     * 以 MySQL 为准重建全局热度 ZSet（快照式：先清空再写 TopN），修复漂移并清理已软删/陈旧成员。
     *
     * @param topN 保留的 TopN 条数
     */
    void rebuild(int topN);

    /** 缓存总开关。 */
    boolean isCacheEnabled();
}
