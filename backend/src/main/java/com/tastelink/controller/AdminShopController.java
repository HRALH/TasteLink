package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.request.UpdateShopRequest;
import com.tastelink.dto.response.ShopDetailVO;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.AdminShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员后台接口（v2 Phase B）。
 * 路径前缀 {@code /api/v1/admin/**} 由 {@code SecurityConfig} 统一要求 hasRole('ADMIN')，
 * 故控制器内不再重复方法级鉴权；普通用户访问将得到 403。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/admin/shops")
@RequiredArgsConstructor
public class AdminShopController {

    private final AdminShopService adminShopService;

    /** 编辑店铺。并发冲突返回 409 {@code SHOP_VERSION_CONFLICT}，提示刷新重试。 */
    @PutMapping("/{shopId}")
    public R<ShopDetailVO> updateShop(@PathVariable Long shopId,
                                      @Valid @RequestBody UpdateShopRequest req) {
        // 确保登录态（虽 admin 规则已隐含 authenticated，不登录会被 SecurityConfig 401 拦下）
        SecurityContextHelper.requireCurrentUserId();
        return R.ok(adminShopService.updateShop(shopId, req));
    }
}
