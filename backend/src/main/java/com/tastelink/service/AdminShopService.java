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
}
