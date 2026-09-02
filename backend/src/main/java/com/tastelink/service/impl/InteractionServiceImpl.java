package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.CommentVO;
import com.tastelink.dto.response.LikeCountVO;
import com.tastelink.entity.Review;
import com.tastelink.entity.ReviewComment;
import com.tastelink.entity.ReviewLike;
import com.tastelink.entity.Shop;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ReviewCommentMapper;
import com.tastelink.mapper.ReviewLikeMapper;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.HotRankService;
import com.tastelink.service.InteractionService;
import com.tastelink.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final ReviewMapper reviewMapper;
    private final ReviewLikeMapper reviewLikeMapper;
    private final ReviewCommentMapper reviewCommentMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final HotRankService hotRankService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeCountVO like(Long reviewId, Long userId) {
        Review review = mustGetReview(reviewId);
        try {
            ReviewLike like = new ReviewLike();
            like.setReviewId(reviewId);
            like.setUserId(userId);
            reviewLikeMapper.insert(like);
            // 点评点赞数 +1；店铺累计点赞数 +1（热度次级排序依据）
            reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                    .eq(Review::getId, reviewId)
                    .setSql("like_count = like_count + 1"));
            shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                    .eq(Shop::getId, review.getShopId())
                    .setSql("like_count = like_count + 1"));
            // 热度缓存：仅在真正新增点赞（非幂等重复）后更新；于事务提交后执行，DB 回滚则不增（Phase A）
            afterCommit(() -> hotRankService.onLike(reviewId));
        } catch (DuplicateKeyException dup) {
            // 已点赞：幂等返回当前计数，不报错（缓存亦不动）
        }
        Integer count = reviewMapper.selectById(reviewId).getLikeCount();
        return new LikeCountVO(count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeCountVO unlike(Long reviewId, Long userId) {
        Review review = mustGetReview(reviewId);
        int affected = reviewLikeMapper.delete(new LambdaQueryWrapper<ReviewLike>()
                .eq(ReviewLike::getReviewId, reviewId)
                .eq(ReviewLike::getUserId, userId));
        if (affected > 0) {
            reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                    .eq(Review::getId, reviewId)
                    .setSql("like_count = GREATEST(0, like_count - 1)"));
            shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                    .eq(Shop::getId, review.getShopId())
                    .setSql("like_count = GREATEST(0, like_count - 1)"));
            // 仅在确实取消了点赞时回退缓存；事务提交后执行（Phase A）
            afterCommit(() -> hotRankService.onUnlike(reviewId));
        }
        Integer count = reviewMapper.selectById(reviewId).getLikeCount();
        return new LikeCountVO(count);
    }

    @Override
    public PageResult<CommentVO> listComments(Long reviewId, PageQuery pq) {
        Page<ReviewComment> page = new Page<>(pq.getPageOrDefault(), pq.getSizeOrDefault());
        reviewCommentMapper.selectPage(page, new LambdaQueryWrapper<ReviewComment>()
                .eq(ReviewComment::getReviewId, reviewId)
                .eq(ReviewComment::getStatus, Constants.STATUS_NORMAL)
                .orderByAsc(ReviewComment::getCreateTime));
        List<ReviewComment> comments = page.getRecords();
        Map<Long, User> userMap = batchUsers(comments.stream()
                .map(ReviewComment::getUserId)
                .distinct()
                .toList());
        List<CommentVO> vos = comments.stream()
                .map(c -> toCommentVO(c, userMap.get(c.getUserId())))
                .toList();
        return PageResult.from(page, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO createComment(Long reviewId, Long userId, String content) {
        mustGetReview(reviewId);
        ReviewComment c = new ReviewComment();
        c.setReviewId(reviewId);
        c.setUserId(userId);
        c.setContent(content);
        c.setStatus(Constants.STATUS_NORMAL);
        reviewCommentMapper.insert(c);
        // 点评评论数 +1
        reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                .eq(Review::getId, reviewId)
                .setSql("reply_count = reply_count + 1"));
        return toCommentVO(c, userMapper.selectById(userId));
    }

    private Review mustGetReview(Long reviewId) {
        Review r = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getId, reviewId)
                .eq(Review::getStatus, Constants.STATUS_NORMAL));
        if (r == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "点评不存在");
        }
        return r;
    }

    private Map<Long, User> batchUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private CommentVO toCommentVO(ReviewComment c, User u) {
        return CommentVO.builder()
                .id(c.getId())
                .reviewId(c.getReviewId())
                .userId(c.getUserId())
                .userNickname(u == null ? null : u.getNickname())
                .userAvatarUrl(u == null ? null : u.getAvatarUrl())
                .content(c.getContent())
                .createTime(DateUtil.format(c.getCreateTime()))
                .build();
    }

    /**
     * 事务提交后执行 action；无活动事务则立即执行（降级）。Redis 热度写不进 DB 事务，
     * 保证 DB 回滚不会让缓存计数先增；Redis 自身异常在 {@link HotRankService} 内吞掉并靠对账修复。
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
