package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tastelink.entity.Review;
import com.tastelink.entity.ReviewComment;
import com.tastelink.entity.ReviewImage;
import com.tastelink.entity.ReviewLike;
import com.tastelink.entity.Shop;
import com.tastelink.mapper.ReviewCommentMapper;
import com.tastelink.mapper.ReviewImageMapper;
import com.tastelink.mapper.ReviewLikeMapper;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.FileStorageService;
import com.tastelink.service.HotRankService;
import com.tastelink.service.ShopCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 店铺物理清理实现（v2 Phase C）：由 {@code ShopCleanupConsumer}（MQ）/ 对账调度调用。
 * <p>
 * 幂等口径：{@code shopMapper.selectById==null} 即视为已清理，直接返回。物理删为
 * {@code DELETE}（天然幂等），OSS 删除与 ZSet zrem 已在各自实现内吞异常/对账兜底，故重复投递安全。
 * 删图优先用 {@code t_review_image.oss_key} 列值（B1-1 起真实入库），历史空串行回退
 * {@link FileStorageService#ossKeyFromUrl(String)} 反推；失败由各实现静默吞掉、记入对账后台再补，不阻断主流程。
 * <p>
 * 列删除顺序满足外键依赖：image → like → comment → review → shop（全逻辑外键无物理 FK，
 * 但依然按依赖序删便于排查）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopCleanupServiceImpl implements ShopCleanupService {

    private final ShopMapper shopMapper;
    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final ReviewLikeMapper reviewLikeMapper;
    private final ReviewCommentMapper reviewCommentMapper;
    private final FileStorageService fileStorageService;
    private final HotRankService hotRankService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanup(Long shopId) {
        if (shopId == null) {
            return;
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            // 已清理（或从未存在）→ 幂等 no-op
            return;
        }
        // 该店所有点评（含标记阶段置 0 的隐藏态，一并物理删）
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getShopId, shopId));
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        if (!reviewIds.isEmpty()) {
            // 先删 OSS/本地图片（best-effort 吞异常），再做 DB 物删；重复投递时文件已删→delete 为 no-op
            List<ReviewImage> images = reviewImageMapper.selectList(new LambdaQueryWrapper<ReviewImage>()
                    .in(ReviewImage::getReviewId, reviewIds));
            for (ReviewImage img : images) {
                // B1-1 起 oss_key 真实入库，直接用列值；历史空串行回退 URL 反推
                String key = StringUtils.hasText(img.getOssKey())
                        ? img.getOssKey()
                        : fileStorageService.ossKeyFromUrl(img.getUrl());
                if (StringUtils.hasText(key)) {
                    fileStorageService.delete(key);
                }
            }
            // 按外→内物理删行；entity=null wrapper 删不走乐观锁路径，互不影响
            reviewImageMapper.delete(new LambdaQueryWrapper<ReviewImage>().in(ReviewImage::getReviewId, reviewIds));
            reviewLikeMapper.delete(new LambdaQueryWrapper<ReviewLike>().in(ReviewLike::getReviewId, reviewIds));
            reviewCommentMapper.delete(new LambdaQueryWrapper<ReviewComment>().in(ReviewComment::getReviewId, reviewIds));
            reviewMapper.delete(new LambdaQueryWrapper<Review>().in(Review::getId, reviewIds));
            // 热度缓存摘除失效 reviewId（rebuild 仍兜底）
            for (Long rid : reviewIds) {
                hotRankService.onDelete(rid);
            }
        }
        // 删店铺行（乐观锁拦截器不影响 DELETE/物理删）
        shopMapper.deleteById(shopId);
        log.info("shop physically cleaned: shopId={}, reviews={}", shopId, reviewIds.size());
    }
}
