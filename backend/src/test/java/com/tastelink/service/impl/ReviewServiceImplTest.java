package com.tastelink.service.impl;

import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.CreateReviewRequest;
import com.tastelink.dto.response.ReviewVO;
import com.tastelink.entity.Review;
import com.tastelink.entity.ReviewImage;
import com.tastelink.entity.Shop;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ReviewImageMapper;
import com.tastelink.mapper.ReviewLikeMapper;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.FileStorageService;
import com.tastelink.service.HotRankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReviewServiceImpl.createReview 单测（Mockito，不依赖 Docker）。
 * B1-1：恶意图片 URL 入库前 400 拒绝（且不产生任何写库）；合法 URL 真实入库 oss_key 并批量插入（B3-3）。
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewImageMapper reviewImageMapper;
    @Mock
    private ReviewLikeMapper reviewLikeMapper;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private HotRankService hotRankService;
    @Mock
    private FileStorageService fileStorageService;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewMapper, reviewImageMapper, reviewLikeMapper,
                shopMapper, userMapper, hotRankService, fileStorageService);
    }

    private Shop shop() {
        Shop s = new Shop();
        s.setId(1L);
        s.setName("测试店");
        s.setCity("上海");
        return s;
    }

    private CreateReviewRequest req(List<String> imageUrls) {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setContent("味道不错");
        req.setRating(5);
        req.setImageUrls(imageUrls);
        return req;
    }

    @Test
    void createReview_maliciousImageUrl_throws400BeforeAnyWrite() {
        when(shopMapper.selectOne(any())).thenReturn(shop());
        String evil = "http://localhost:8080/static/uploads/../../etc/passwd";
        when(fileStorageService.isOwnedUrl(evil)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createReview(1L, req(List.of(evil)), 7L));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        // 校验失败在写库前抛出：点评/图片/计数均不落库
        verify(reviewMapper, never()).insert(any(Review.class));
        verify(reviewImageMapper, never()).insertBatch(anyList());
        verify(shopMapper, never()).update(any(), any());
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void createReview_foreignImageUrl_throws400() {
        when(shopMapper.selectOne(any())).thenReturn(shop());
        String foreign = "https://evil.example.com/a.jpg";
        when(fileStorageService.isOwnedUrl(foreign)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createReview(1L, req(List.of(foreign)), 7L));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void createReview_validImages_persistsOssKeyInBatch() {
        Shop s = shop();
        when(shopMapper.selectOne(any())).thenReturn(s);
        String url1 = "http://localhost:8080/static/uploads/2026/09/a.jpg";
        String url2 = "http://localhost:8080/static/uploads/2026/09/b.png";
        when(fileStorageService.isOwnedUrl(url1)).thenReturn(true);
        when(fileStorageService.isOwnedUrl(url2)).thenReturn(true);
        when(fileStorageService.ossKeyFromUrl(url1)).thenReturn("2026/09/a.jpg");
        when(fileStorageService.ossKeyFromUrl(url2)).thenReturn("2026/09/b.png");
        when(reviewMapper.insert(any(Review.class))).thenAnswer(inv -> {
            ((Review) inv.getArgument(0)).setId(100L);
            return 1;
        });
        // assemble() 依赖的批量查询
        when(reviewImageMapper.selectList(any())).thenReturn(List.of());
        when(shopMapper.selectBatchIds(any())).thenReturn(List.of(s));
        User u = new User();
        u.setId(7L);
        u.setNickname("tom");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(u));
        when(reviewLikeMapper.selectList(any())).thenReturn(List.of());

        ReviewVO vo = service.createReview(1L, req(List.of(url1, url2)), 7L);

        assertEquals(100L, vo.getId());
        ArgumentCaptor<List<ReviewImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(reviewImageMapper).insertBatch(captor.capture());
        List<ReviewImage> images = captor.getValue();
        assertEquals(2, images.size());
        assertEquals(100L, images.get(0).getReviewId());
        assertEquals(url1, images.get(0).getUrl());
        assertEquals("2026/09/a.jpg", images.get(0).getOssKey());  // oss_key 真实入库
        assertEquals(0, images.get(0).getSortOrder());
        assertEquals("2026/09/b.png", images.get(1).getOssKey());
        assertEquals(1, images.get(1).getSortOrder());
    }

    @Test
    void createReview_noImages_skipsBatchInsert() {
        Shop s = shop();
        when(shopMapper.selectOne(any())).thenReturn(s);
        when(reviewMapper.insert(any(Review.class))).thenAnswer(inv -> {
            ((Review) inv.getArgument(0)).setId(100L);
            return 1;
        });
        when(reviewImageMapper.selectList(any())).thenReturn(List.of());
        when(shopMapper.selectBatchIds(any())).thenReturn(List.of(s));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(new User()));
        when(reviewLikeMapper.selectList(any())).thenReturn(List.of());

        service.createReview(1L, req(null), 7L);

        verify(reviewImageMapper, never()).insertBatch(anyList());
    }
}
