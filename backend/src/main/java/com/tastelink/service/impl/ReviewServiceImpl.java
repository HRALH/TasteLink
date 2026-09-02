package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.CreateReviewRequest;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.ReviewVO;
import com.tastelink.entity.Review;
import com.tastelink.entity.ReviewImage;
import com.tastelink.entity.ReviewLike;
import com.tastelink.entity.Shop;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ReviewImageMapper;
import com.tastelink.mapper.ReviewLikeMapper;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.HotRankService;
import com.tastelink.service.ReviewService;
import com.tastelink.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final ReviewLikeMapper reviewLikeMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final HotRankService hotRankService;

    @Override
    public ReviewVO getDetail(Long reviewId) {
        Review review = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getId, reviewId)
                .eq(Review::getStatus, Constants.STATUS_NORMAL));
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "点评不存在");
        }
        return assemble(List.of(review), SecurityContextHelper.getCurrentUserId()).get(0);
    }

    @Override
    public PageResult<ReviewVO> listByShop(Long shopId, PageQuery pq, String sortBy) {
        Page<Review> page = new Page<>(pq.getPageOrDefault(), pq.getSizeOrDefault());
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<Review>()
                .eq(Review::getShopId, shopId)
                .eq(Review::getStatus, Constants.STATUS_NORMAL);
        if ("like".equalsIgnoreCase(sortBy)) {
            w.orderByDesc(Review::getLikeCount).orderByDesc(Review::getCreateTime);
        } else {
            // 默认时间倒序
            w.orderByDesc(Review::getCreateTime);
        }
        reviewMapper.selectPage(page, w);
        List<ReviewVO> vos = assemble(page.getRecords(), SecurityContextHelper.getCurrentUserId());
        return PageResult.from(page, vos);
    }

    @Override
    public PageResult<ReviewVO> listByUser(Long userId, PageQuery pq) {
        Page<Review> page = new Page<>(pq.getPageOrDefault(), pq.getSizeOrDefault());
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getStatus, Constants.STATUS_NORMAL)
                .orderByDesc(Review::getCreateTime);
        reviewMapper.selectPage(page, w);
        List<ReviewVO> vos = assemble(page.getRecords(), SecurityContextHelper.getCurrentUserId());
        return PageResult.from(page, vos);
    }

    @Override
    public List<ReviewVO> recentByShop(Long shopId, int limit) {
        Page<Review> page = new Page<>(1, limit, false);
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<Review>()
                .eq(Review::getShopId, shopId)
                .eq(Review::getStatus, Constants.STATUS_NORMAL)
                .orderByDesc(Review::getCreateTime);
        reviewMapper.selectPage(page, w);
        return assemble(page.getRecords(), SecurityContextHelper.getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO createReview(Long shopId, CreateReviewRequest req, Long userId) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .eq(Shop::getStatus, Constants.STATUS_NORMAL));
        if (shop == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }

        Review review = new Review();
        review.setShopId(shopId);
        review.setUserId(userId);
        review.setCity(shop.getCity());
        review.setContent(req.getContent());
        review.setRating(req.getRating());
        review.setLikeCount(0);
        review.setReplyCount(0);
        review.setStatus(Constants.STATUS_NORMAL);
        reviewMapper.insert(review);

        // 图片（有序）
        List<String> imageUrls = req.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            int order = 0;
            for (String url : imageUrls) {
                if (!StringUtils.hasText(url)) {
                    continue;
                }
                ReviewImage img = new ReviewImage();
                img.setReviewId(review.getId());
                img.setUrl(url);
                img.setOssKey("");
                img.setSortOrder(order++);
                reviewImageMapper.insert(img);
            }
        }

        // 店铺计数：点评数 +1、评分汇总累加、平均评分重算（单条 UPDATE 内顺序求值）
        int r = req.getRating();
        shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .setSql("review_count = review_count + 1, rating_sum = rating_sum + " + r
                        + ", avg_rating = ROUND(rating_sum / review_count, 2)"));

        // 用户点评数 +1
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("review_count = review_count + 1"));

        return assemble(List.of(review), userId).get(0);
    }

    @Override
    public List<ReviewVO> hotReviews(String city, int limit) {
        // 无城市筛选时优先走 Redis 全局热度缓存（Phase A）；其余/未命中/异常回退 MySQL
        if (!StringUtils.hasText(city) && hotRankService.isCacheEnabled()) {
            List<Long> ids = hotRankService.topReviewIds(limit);
            if (ids != null && !ids.isEmpty()) {
                Map<Long, Review> byId = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                                .in(Review::getId, ids)
                                .eq(Review::getStatus, Constants.STATUS_NORMAL))
                        .stream().collect(Collectors.toMap(Review::getId, rv -> rv));
                // 按缓存热度顺序还原；跳过因软删/不存在而缺失的 id
                List<Review> ordered = new ArrayList<>();
                for (Long id : ids) {
                    Review rv = byId.get(id);
                    if (rv != null) {
                        ordered.add(rv);
                    }
                }
                if (!ordered.isEmpty()) {
                    return assemble(ordered, SecurityContextHelper.getCurrentUserId());
                }
            }
        }
        return hotReviewsFromDb(city, limit);
    }

    /** MySQL 热度排序（缓存禁用 / 城市筛选 / 缓存未命中时走此路径）。 */
    private List<ReviewVO> hotReviewsFromDb(String city, int limit) {
        Page<Review> page = new Page<>(1, limit, false);
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<Review>()
                .eq(Review::getStatus, Constants.STATUS_NORMAL);
        if (StringUtils.hasText(city)) {
            w.eq(Review::getCity, city);
        }
        w.orderByDesc(Review::getLikeCount).orderByDesc(Review::getReplyCount);
        reviewMapper.selectPage(page, w);
        return assemble(page.getRecords(), SecurityContextHelper.getCurrentUserId());
    }

    /**
     * 批量装配 ReviewVO：一次取店铺/用户/图片/点赞标志，避免 N+1。
     */
    private List<ReviewVO> assemble(List<Review> reviews, Long currentUserId) {
        if (reviews == null || reviews.isEmpty()) {
            return List.of();
        }
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        List<Long> shopIds = reviews.stream().map(Review::getShopId).distinct().toList();
        List<Long> userIds = reviews.stream().map(Review::getUserId).distinct().toList();

        Map<Long, List<String>> imgMap = reviewImageMapper.selectList(
                        new LambdaQueryWrapper<ReviewImage>()
                                .in(ReviewImage::getReviewId, reviewIds)
                                .orderByAsc(ReviewImage::getSortOrder))
                .stream().collect(Collectors.groupingBy(
                        ReviewImage::getReviewId,
                        Collectors.mapping(ReviewImage::getUrl, Collectors.toList())));

        Map<Long, String> shopNameMap = shopIds.isEmpty() ? Map.of()
                : shopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName));

        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> likedIds = Collections.emptySet();
        if (currentUserId != null) {
            likedIds = reviewLikeMapper.selectList(new LambdaQueryWrapper<ReviewLike>()
                            .eq(ReviewLike::getUserId, currentUserId)
                            .in(ReviewLike::getReviewId, reviewIds))
                    .stream().map(ReviewLike::getReviewId).collect(Collectors.toSet());
        }
        final Set<Long> finalLiked = likedIds;

        return reviews.stream().map(rv -> {
            User u = userMap.get(rv.getUserId());
            return ReviewVO.builder()
                    .id(rv.getId())
                    .shopId(rv.getShopId())
                    .shopName(shopNameMap.get(rv.getShopId()))
                    .userId(rv.getUserId())
                    .userNickname(u == null ? null : u.getNickname())
                    .userAvatarUrl(u == null ? null : u.getAvatarUrl())
                    .content(rv.getContent())
                    .rating(rv.getRating())
                    .likeCount(rv.getLikeCount())
                    .replyCount(rv.getReplyCount())
                    .images(imgMap.getOrDefault(rv.getId(), List.of()))
                    .hasLiked(finalLiked.contains(rv.getId()))
                    .createTime(DateUtil.format(rv.getCreateTime()))
                    .build();
        }).toList();
    }
}
