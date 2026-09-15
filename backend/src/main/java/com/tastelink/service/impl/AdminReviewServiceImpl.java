package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.ResultCode;
import com.tastelink.dto.response.AdminReviewVO;
import com.tastelink.entity.Review;
import com.tastelink.entity.Shop;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.AdminReviewService;
import com.tastelink.service.HotRankService;
import com.tastelink.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 后台点评治理（产品优化 F4）。下架/恢复均对称维护 shop.review_count/rating_sum/avg_rating 与
 * user.review_count（镜像 {@link ReviewServiceImpl#createReview} 的累加与
 * {@link AdminShopServiceImpl#deleteShop} 的回扣），GREATEST(0) 防负；avg 显式重算。
 * 不物理删，保留数据可恢复。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewMapper reviewMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final HotRankService hotRankService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hideReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null || review.getStatus() == Constants.STATUS_HIDDEN) {
            throw new BusinessException(ResultCode.NOT_FOUND, "点评不存在或已下架");
        }
        int rating = Optional.ofNullable(review.getRating()).orElse(0);
        Long shopId = review.getShopId();
        Long authorId = review.getUserId();

        // 1) 标记下架
        review.setStatus(Constants.STATUS_HIDDEN);
        reviewMapper.updateById(review);

        // 2) 镜像回扣 shop 计数 + 重算 avg
        shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .setSql("review_count = GREATEST(0, review_count - 1), "
                        + "rating_sum = GREATEST(0, rating_sum - " + rating + ")"));
        shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .setSql("avg_rating = IF(review_count = 0, 0.00, ROUND(rating_sum / review_count, 2))"));

        // 3) 镜像回扣作者 review_count
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, authorId)
                .setSql("review_count = GREATEST(0, review_count - 1)"));

        // 4) 从热度 ZSet 摘除（afterCommit；Redis IO 不进事务；HotRankService 内部吞异常 + rebuild 兜底）
        afterCommit(() -> hotRankService.onDelete(reviewId));
    }

    @Override
    public PageResult<AdminReviewVO> listReviews(Integer page, Integer size, Integer status) {
        int p = page == null || page < 1 ? Constants.DEFAULT_PAGE : page;
        int s = size == null || size < 1 ? Constants.DEFAULT_SIZE : Math.min(size, Constants.MAX_SIZE);
        Page<Review> pg = new Page<>(p, s);
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<Review>()
                .orderByDesc(Review::getCreateTime);
        if (status != null) {
            w.eq(Review::getStatus, status);
        }
        reviewMapper.selectPage(pg, w);
        List<Review> rows = pg.getRecords();
        // 批量回查店铺名 + 作者昵称
        List<Long> shopIds = rows.stream().map(Review::getShopId).distinct().toList();
        List<Long> userIds = rows.stream().map(Review::getUserId).distinct().toList();
        Map<Long, String> shopNameMap = shopIds.isEmpty() ? Map.of()
                : shopMapper.selectBatchIds(shopIds).stream()
                    .collect(Collectors.toMap(Shop::getId, Shop::getName));
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        List<AdminReviewVO> vos = rows.stream().map(rv -> AdminReviewVO.builder()
                .id(rv.getId())
                .shopId(rv.getShopId())
                .shopName(shopNameMap.get(rv.getShopId()))
                .userId(rv.getUserId())
                .userNickname(userMap.get(rv.getUserId()) == null ? null : userMap.get(rv.getUserId()).getNickname())
                .content(rv.getContent())
                .rating(rv.getRating())
                .likeCount(rv.getLikeCount())
                .replyCount(rv.getReplyCount())
                .status(rv.getStatus())
                .createTime(DateUtil.format(rv.getCreateTime()))
                .build()).toList();
        return PageResult.from(pg, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null || review.getStatus() == Constants.STATUS_NORMAL) {
            throw new BusinessException(ResultCode.NOT_FOUND, "点评未下架或不存在");
        }
        int rating = Optional.ofNullable(review.getRating()).orElse(0);
        Long shopId = review.getShopId();
        Long authorId = review.getUserId();

        // 仅当店铺仍正常时才恢复计数，避免被删店铺计数虚增；店铺 status≠1 视为不存在
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || shop.getStatus() == Constants.STATUS_HIDDEN) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在或已下架，无法恢复点评");
        }

        // 1) 恢复可见
        review.setStatus(Constants.STATUS_NORMAL);
        reviewMapper.updateById(review);

        // 2) 镜像加回 shop 计数 + 重算 avg
        shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .setSql("review_count = review_count + 1, rating_sum = rating_sum + " + rating));
        shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .setSql("avg_rating = IF(review_count = 0, 0.00, ROUND(rating_sum / review_count, 2))"));

        // 3) 镜像加回作者 review_count
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, authorId)
                .setSql("review_count = review_count + 1"));
    }

    /**
     * 事务提交后执行 action；无活动事务则立即执行（降级）。与 HotRank/删店同手法（第 4 份 copy，
     * 沿用既有约定不抽公共工具）。Redis IO 不进 DB 事务，保证 DB 回滚不会让缓存先于 DB 状态变化。
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
