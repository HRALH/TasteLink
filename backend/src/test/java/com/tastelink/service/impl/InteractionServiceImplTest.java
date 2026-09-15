package com.tastelink.service.impl;

import com.tastelink.dto.response.LikeCountVO;
import com.tastelink.entity.Review;
import com.tastelink.entity.ReviewLike;
import com.tastelink.mapper.ReviewCommentMapper;
import com.tastelink.mapper.ReviewLikeMapper;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.HotRankService;
import com.tastelink.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * InteractionServiceImpl.like 幂等分支单测（B2-5）。
 * 重复点赞撞 uk_review_user → 吞 DuplicateKeyException 并返回当前计数；
 * 计数不变、热度缓存不动（非嵌套事务前提见实现注释）。
 */
@ExtendWith(MockitoExtension.class)
class InteractionServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewLikeMapper reviewLikeMapper;
    @Mock
    private ReviewCommentMapper reviewCommentMapper;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private HotRankService hotRankService;
    @Mock
    private NotificationService notificationService;

    private InteractionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InteractionServiceImpl(reviewMapper, reviewLikeMapper, reviewCommentMapper,
                shopMapper, userMapper, hotRankService, notificationService);
    }

    @Test
    void like_duplicate_returnsCurrentCountWithoutSideEffects() {
        Review review = new Review();
        review.setId(10L);
        review.setShopId(1L);
        review.setLikeCount(5);
        review.setStatus(com.tastelink.common.Constants.STATUS_NORMAL);
        when(reviewMapper.selectOne(any())).thenReturn(review);
        when(reviewLikeMapper.insert(any(ReviewLike.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry '10-7' for key 'uk_review_user'"));
        when(reviewMapper.selectById(10L)).thenReturn(review);

        LikeCountVO vo = service.like(10L, 7L);

        assertEquals(5, vo.likeCount(), "重复点赞返回当前计数");
        verify(reviewMapper, never()).update(any(), any());
        verify(shopMapper, never()).update(any(), any());
        verifyNoInteractions(hotRankService);   // 缓存不动
    }
}
