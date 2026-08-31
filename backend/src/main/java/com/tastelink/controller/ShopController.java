package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.request.CreateReviewRequest;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.CategoryVO;
import com.tastelink.common.PageResult;
import com.tastelink.dto.response.ReviewVO;
import com.tastelink.dto.response.ShopDetailVO;
import com.tastelink.dto.response.ShopVO;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.ReviewService;
import com.tastelink.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店铺：列表/详情/分类字典，以及店铺下的点评列表与发布点评。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final ReviewService reviewService;

    @GetMapping
    public R<PageResult<ShopVO>> listShops(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sortBy,
            PageQuery pq) {
        return R.ok(shopService.listShops(keyword, categoryId, city, sortBy, pq));
    }

    @GetMapping("/categories")
    public R<List<CategoryVO>> categories() {
        return R.ok(shopService.listCategories());
    }

    @GetMapping("/{shopId}")
    public R<ShopDetailVO> getShopDetail(@PathVariable Long shopId) {
        return R.ok(shopService.getShopDetail(shopId));
    }

    @GetMapping("/{shopId}/reviews")
    public R<PageResult<ReviewVO>> listShopReviews(@PathVariable Long shopId,
                                                    @RequestParam(required = false) String sortBy,
                                                    PageQuery pq) {
        return R.ok(reviewService.listByShop(shopId, pq, sortBy));
    }

    @PostMapping("/{shopId}/reviews")
    public R<ReviewVO> createReview(@PathVariable Long shopId, @Valid @RequestBody CreateReviewRequest req) {
        Long userId = SecurityContextHelper.requireCurrentUserId();
        return R.ok(reviewService.createReview(shopId, req, userId));
    }
}
