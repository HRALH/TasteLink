package com.tastelink.service;

/**
 * 店铺物理清理服务（v2 Phase C）。
 * <p>
 * 消费者/对账调度的统一执行体：幂等地物理级联删 image/like/comment/review/shop、删 OSS、清热度。
 * 重复调用同 {@code shopId} 安全：店铺行不存在即视为已清理直接 no-op。
 */
public interface ShopCleanupService {

    /** 按 shopId 执行物理级联清理（幂等）。 */
    void cleanup(Long shopId);
}
