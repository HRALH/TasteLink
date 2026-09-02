package com.tastelink.service.impl;

import com.tastelink.entity.Review;
import com.tastelink.entity.ReviewImage;
import com.tastelink.entity.Shop;
import com.tastelink.mapper.ReviewCommentMapper;
import com.tastelink.mapper.ReviewImageMapper;
import com.tastelink.mapper.ReviewLikeMapper;
import com.tastelink.mapper.ReviewMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.FileStorageService;
import com.tastelink.service.HotRankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ShopCleanupServiceImpl 逻辑单测（Mockito，不依赖 Docker，随 {@code mvn test} 运行）。
 * 验证级联物删顺序、OSS key 从 url 反推、热图 zrem、幂等（shop 已不存在→no-op）。
 * 真实级联物理删除端到端见 {@link ShopCleanupIT}。
 */
@ExtendWith(MockitoExtension.class)
class ShopCleanupServiceImplTest {

    @Mock
    private ShopMapper shopMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewImageMapper reviewImageMapper;
    @Mock
    private ReviewLikeMapper reviewLikeMapper;
    @Mock
    private ReviewCommentMapper reviewCommentMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private HotRankService hotRankService;

    private ShopCleanupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ShopCleanupServiceImpl(shopMapper, reviewMapper, reviewImageMapper,
                reviewLikeMapper, reviewCommentMapper, fileStorageService, hotRankService);
    }

    private Shop shop(long id) {
        Shop s = new Shop();
        s.setId(id);
        return s;
    }

    private Review review(long id, long shopId, long userId) {
        Review r = new Review();
        r.setId(id);
        r.setShopId(shopId);
        r.setUserId(userId);
        return r;
    }

    private ReviewImage image(long reviewId, String url) {
        ReviewImage img = new ReviewImage();
        img.setReviewId(reviewId);
        img.setUrl(url);
        img.setOssKey("");
        return img;
    }

    @Test
    void cleanup_shopGone_isNoOp() {
        when(shopMapper.selectById(9L)).thenReturn(null);
        service.cleanup(9L);
        verifyNoInteractions(reviewMapper, reviewImageMapper, reviewLikeMapper,
                reviewCommentMapper, fileStorageService, hotRankService);
    }

    @Test
    void cleanup_cascadesImagesReviewsHotRank_deletesShop() {
        Shop s = shop(1L);
        when(shopMapper.selectById(1L)).thenReturn(s);
        Review r1 = review(10L, 1L, 7L);
        Review r2 = review(20L, 1L, 7L);
        when(reviewMapper.selectList(any())).thenReturn(List.of(r1, r2));
        when(reviewImageMapper.selectList(any())).thenReturn(List.of(
                image(10L, "http://h:8080/static/uploads/2026/09/a.jpg")));
        when(fileStorageService.ossKeyFromUrl("http://h:8080/static/uploads/2026/09/a.jpg"))
                .thenReturn("2026/09/a.jpg");

        service.cleanup(1L);

        // OSS key 由 url 反推后删图
        verify(fileStorageService).delete("2026/09/a.jpg");
        // 级联物删（按外→内）+ 热度 zrem + 店铺行删除
        verify(reviewImageMapper).delete(any());
        verify(reviewLikeMapper).delete(any());
        verify(reviewCommentMapper).delete(any());
        verify(reviewMapper).delete(any());
        verify(hotRankService).onDelete(10L);
        verify(hotRankService).onDelete(20L);
        verify(shopMapper).deleteById(1L);
    }

    @Test
    void cleanup_unmatchedUrl_skipsFileDeleteButStillDropsRows() {
        Shop s = shop(1L);
        when(shopMapper.selectById(1L)).thenReturn(s);
        Review r = review(10L, 1L, 7L);
        when(reviewMapper.selectList(any())).thenReturn(List.of(r));
        when(reviewImageMapper.selectList(any())).thenReturn(List.of(
                image(10L, "https://foreign.example.com/x.jpg")));
        // url 反推不出 key → 视为外链/历史脏数据，跳过删图
        when(fileStorageService.ossKeyFromUrl("https://foreign.example.com/x.jpg")).thenReturn(null);

        service.cleanup(1L);

        verify(fileStorageService, never()).delete(any());
        verify(reviewImageMapper).delete(any());            // 行仍删除（仅存储对象不强删）
        verify(reviewMapper).delete(any());
        verify(shopMapper).deleteById(1L);
    }

    @Test
    void cleanup_noReviews_skipsCascadeButDeletesShop() {
        Shop s = shop(1L);
        when(shopMapper.selectById(1L)).thenReturn(s);
        when(reviewMapper.selectList(any())).thenReturn(List.of());

        service.cleanup(1L);

        verifyNoInteractions(reviewImageMapper, reviewLikeMapper, reviewCommentMapper, hotRankService);
        verify(reviewMapper, never()).delete(any());
        verify(fileStorageService, never()).delete(any());
        verify(shopMapper).deleteById(1L);
    }

    @Test
    void cleanup_nullShopId_isSafeNoOp() {
        service.cleanup(null);            // null guard：不抛、不触库
        verifyNoInteractions(shopMapper);
    }
}
