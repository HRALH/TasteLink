package com.tastelink.service.impl;

import com.tastelink.dto.response.HomeVO;
import com.tastelink.service.RecommendService;
import com.tastelink.service.ReviewService;
import com.tastelink.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 首页实现：复用 shopService.hotShops 与 reviewService.hotReviews，各取 10 条。
 */
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private static final int TOP = 10;

    private final ShopService shopService;
    private final ReviewService reviewService;

    @Override
    public HomeVO home(String city) {
        return HomeVO.builder()
                .hotShops(shopService.hotShops(city, TOP))
                .hotReviews(reviewService.hotReviews(city, TOP))
                .build();
    }
}
