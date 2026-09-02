package com.tastelink.service.impl;

import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.UpdateShopRequest;
import com.tastelink.dto.response.ShopDetailVO;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopCategory;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ShopCategoryMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.AdminShopService;
import com.tastelink.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理员后台服务实现（v2 Phase B）。
 * <p>
 * 乐观锁以 MyBatis-Plus {@link com.baomidou.mybatisplus.annotation.Version} 实现：
 * {@link ShopMapper#selectById(Object)} 载入的 shop 携带当前 version，{@link ShopMapper#updateById(Object)}
 * 在 {@link com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor} 作用下
 * 生成 {@code UPDATE ... SET version=?+1 WHERE id=? AND version=?}；版本不符则影响行数 0（注意 MP 不抛
 * OptimisticLockingFailureException，故此处据 affected==0 判定冲突），映射为 409。
 */
@Service
@RequiredArgsConstructor
public class AdminShopServiceImpl implements AdminShopService {

    private final ShopMapper shopMapper;
    private final ShopCategoryMapper categoryMapper;
    private final ShopService shopService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopDetailVO updateShop(Long shopId, UpdateShopRequest req) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }
        // 覆盖可改字段：字符串非空才更新（口径同 UpdateProfileRequest），description 可清空
        if (StringUtils.hasText(req.getName())) {
            shop.setName(req.getName());
        }
        if (req.getCategoryId() != null) {
            // 逻辑外键引用存在性校验（无物理 FK）
            ShopCategory cat = categoryMapper.selectById(req.getCategoryId());
            if (cat == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "店铺分类不存在");
            }
            shop.setCategoryId(req.getCategoryId());
        }
        if (StringUtils.hasText(req.getCity())) {
            shop.setCity(req.getCity());
        }
        if (StringUtils.hasText(req.getAddress())) {
            shop.setAddress(req.getAddress());
        }
        if (StringUtils.hasText(req.getPhone())) {
            shop.setPhone(req.getPhone());
        }
        if (StringUtils.hasText(req.getCoverUrl())) {
            shop.setCoverUrl(req.getCoverUrl());
        }
        if (req.getDescription() != null) {
            shop.setDescription(req.getDescription());
        }

        int affected = shopMapper.updateById(shop);
        if (affected == 0) {
            // 并发冲突：他人已在本次读取后提交，version 不符；要求调用方刷新后重试
            throw new BusinessException(ResultCode.SHOP_VERSION_CONFLICT);
        }
        // 复用公开读路径的详情组装（含分类名与近期点评）；仅 status=1 的店铺可见详情，
        // 管理员对下架店铺的编辑由 Phase C 软删补完，此处面向正常店铺。
        return shopService.getShopDetail(shopId);
    }
}
