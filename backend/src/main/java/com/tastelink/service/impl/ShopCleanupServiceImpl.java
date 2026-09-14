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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 店铺物理清理实现（v2 Phase C）：由 {@code ShopCleanupConsumer}（MQ）/ 对账调度调用。
 * <p>
 * 幂等口径：{@code shopMapper.selectById==null} 即视为已清理，直接返回。物理删为
 * {@code DELETE}（天然幂等），OSS 删除与 ZSet zrem 已在各自实现内吞异常/对账兜底，故重复投递安全。
 * 删图优先用 {@code t_review_image.oss_key} 列值（B1-1 起真实入库），历史空串行回退
 * {@link FileStorageService#ossKeyFromUrl(String)} 反推；失败由各实现静默吞掉、记入对账后台再补，不阻断主流程。
 * <p>
 * B3-2 起事务瘦身：DB 级联物理删留在事务内；OSS 文件删除与 Redis zrem 收集到列表，
 * {@code afterCommit} 再执行（与 Phase A/C 既有 afterCommit 模式一致）。这些外部 IO 不再占着
 * DB 连接与行锁；失败靠下一轮对账重清（幂等已具备）。
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
        // B3-2：OSS/本地图片删除与 Redis zrem 收集到 afterCommit 再执行——
        // 这些是网络 IO，留在事务内会把 DB 连接与行锁拉长；失败靠下一轮对账重清（幂等已具备，
        // DELETE 天然幂等 + selectById==null no-op + OSS/Redis 各自吞异常/对账兜底）
        List<Runnable> afterCommitTasks = new ArrayList<>();
        if (!reviewIds.isEmpty()) {
            // 读出待删图片 key（事务内读，避免删行后取不到）；delete 收集到 afterCommit
            List<ReviewImage> images = reviewImageMapper.selectList(new LambdaQueryWrapper<ReviewImage>()
                    .in(ReviewImage::getReviewId, reviewIds));
            for (ReviewImage img : images) {
                // B1-1 起 oss_key 真实入库，直接用列值；历史空串行回退 URL 反推
                String key = StringUtils.hasText(img.getOssKey())
                        ? img.getOssKey()
                        : fileStorageService.ossKeyFromUrl(img.getUrl());
                if (StringUtils.hasText(key)) {
                    afterCommitTasks.add(() -> fileStorageService.delete(key));
                }
            }
            // 按外→内物理删行；entity=null wrapper 删不走乐观锁路径，互不影响
            reviewImageMapper.delete(new LambdaQueryWrapper<ReviewImage>().in(ReviewImage::getReviewId, reviewIds));
            reviewLikeMapper.delete(new LambdaQueryWrapper<ReviewLike>().in(ReviewLike::getReviewId, reviewIds));
            reviewCommentMapper.delete(new LambdaQueryWrapper<ReviewComment>().in(ReviewComment::getReviewId, reviewIds));
            reviewMapper.delete(new LambdaQueryWrapper<Review>().in(Review::getId, reviewIds));
            // 热度缓存摘除失效 reviewId 收集到 afterCommit（rebuild 仍兜底）
            for (Long rid : reviewIds) {
                afterCommitTasks.add(() -> hotRankService.onDelete(rid));
            }
        }
        // 删店铺行（乐观锁拦截器不影响 DELETE/物理删）
        shopMapper.deleteById(shopId);
        log.info("shop physically cleaned: shopId={}, reviews={}", shopId, reviewIds.size());
        // DB 事务提交后再做 OSS/Redis 外部副作用，回滚则不发（避免脏删文件）
        afterCommit(() -> afterCommitTasks.forEach(Runnable::run));
    }

    /**
     * 事务提交后执行 action；无活动事务则立即执行（降级）。OSS 删除/Redis zrem 不进 DB 事务：
     * 保证 DB 回滚不会发出脏副作用；外部故障由各实现吞掉、对账重清兜底。
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
