package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tastelink.common.Constants;
import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.UpdateShopRequest;
import com.tastelink.dto.response.ShopDetailVO;
import com.tastelink.entity.Review;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopCategory;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopCategoryMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.AdminShopService;
import com.tastelink.service.HotRankService;
import com.tastelink.service.ShopCleanupProducer;
import com.tastelink.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员后台服务实现（v2 Phase B 改店 / Phase C 删店）。
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
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final ShopService shopService;
    private final ShopCleanupProducer shopCleanupProducer;
    private final HotRankService hotRankService;

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
        return shopService.getShopDetail(shopId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || (shop.getStatus() != null && shop.getStatus() == Constants.STATUS_HIDDEN)) {
            // 已删除/下架的店铺二次删除得 404（幂等口风）；不存在同样 404
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }
        // 1）标记下架（走乐观锁，与并发改/删冲突 → 409 提示刷新）
        shop.setStatus(Constants.STATUS_HIDDEN);
        int affected = shopMapper.updateById(shop);
        if (affected == 0) {
            throw new BusinessException(ResultCode.SHOP_VERSION_CONFLICT);
        }
        // 2）级联隐藏该店正常点评 —— 即时从所有公开读路径消失（首页热度亦按 review.status=NORMAL 过滤）
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getShopId, shopId)
                .eq(Review::getStatus, Constants.STATUS_NORMAL));
        if (!reviews.isEmpty()) {
            // setSql 直写（不依赖 lambda .set 的 TableInfo 缓存，纯单测可覆盖；status 为代码常量无注入风险）
            reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                    .eq(Review::getShopId, shopId)
                    .eq(Review::getStatus, Constants.STATUS_NORMAL)
                    .setSql("status = " + Constants.STATUS_HIDDEN));
            // 2.5）B2-2：对称回扣店铺 review_count/rating_sum 并重算 avg_rating（镜像 createReview 的累加，
            // GREATEST 0 防负；avg 显式重算同 B2-3）——否则店铺恢复（status 回 1）后评分/点评数永久虚高
            int cnt = reviews.size();
            int ratingSum = reviews.stream()
                    .mapToInt(rv -> rv.getRating() == null ? 0 : rv.getRating())
                    .sum();
            shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                    .eq(Shop::getId, shopId)
                    .setSql("review_count = GREATEST(0, review_count - " + cnt
                            + "), rating_sum = GREATEST(0, rating_sum - " + ratingSum + ")"));
            shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                    .eq(Shop::getId, shopId)
                    .setSql("avg_rating = IF(review_count = 0, 0.00, ROUND(rating_sum / review_count, 2))"));
            // 3）对称回扣发布用户 review_count（镜像 createReview 的 +1，按用户聚合累减，GREATEST 0 防负）
            Map<Long, Long> perUser = reviews.stream()
                    .collect(Collectors.groupingBy(Review::getUserId, Collectors.counting()));
            perUser.forEach((uid, c) -> userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, uid)
                    .setSql("review_count = GREATEST(0, review_count - " + c + ")")));
        }
        // 3.5）B3-3：标记下架即对被隐藏点评从热度 ZSet 摘除（zrem），不等物理删阶段——
        // 否则 rebuild 窗口期首页热榜仍带这些已隐藏 reviewId，ReviewServiceImpl:166-171 过滤死成员后条数不足。
        // zrem 走 afterCommit（Redis IO 不进 DB 事务；HotRankService.onDelete 内部吞异常 + rebuild 兜底）
        List<Long> hiddenReviewIds = reviews.stream().map(Review::getId).toList();
        afterCommit(() -> hiddenReviewIds.forEach(hotRankService::onDelete));
        // 4）物删清理走延时 MQ 消费者；于此事务提交后投递，DB 回滚则不发
        afterCommit(() -> shopCleanupProducer.send(shopId));
    }

    /**
     * 事务提交后执行 action；无活动事务则立即执行（降级）。MQ 投递不进 DB 事务：保证标记事务回滚
     * 不会发出脏消息；broker 故障由 {@link com.tastelink.service.ShopCleanupProducer} 内部吞掉，
     * 经 {@code ScheduledShopCleanupReconcile} 对账补投递兜底。
     */
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
