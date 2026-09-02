package com.tastelink.service;

import com.tastelink.dto.request.UpdateShopRequest;
import com.tastelink.dto.response.ShopDetailVO;

/**
 * 管理员后台服务（v2 Phase B）。鉴权由 SecurityConfig 的 {@code /api/v1/admin/** hasRole('ADMIN')} 兜底。
 */
public interface AdminShopService {

    /**
     * 编辑店铺：selectById 载入（含 version）→ 覆盖可改字段 → updateById（乐观锁）。
     * 并发冲突时 updateById 影响行数为 0，映射为 {@code SHOP_VERSION_CONFLICT}(409)。
     */
    ShopDetailVO updateShop(Long shopId, UpdateShopRequest req);

    /**
     * 删除店铺（v2 Phase C，C-Full 物理删路径）：
     * 同事务内标记 {@code shop.status=0} + 级联 {@code review.status=0}（即时从公开读路径消失）
     * + 对称回扣发布用户 {@code review_count}（GREATEST 0 防负，前置到标记事务以保即时一致），
     * 然后于 afterCommit 投递延时清理消息；物理级联删 image/like/comment/review/shop、删 OSS、清热度
     * 由异步消费者承担（幂等）。broker 缺失时降级为对账调度直接清理。
     */
    void deleteShop(Long shopId);
}
