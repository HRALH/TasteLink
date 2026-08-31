package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.response.ReviewVO;
import com.tastelink.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 点评详情。店铺下/用户下点评列表与发布已分别在 ShopController / UserController。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{reviewId}")
    public R<ReviewVO> getDetail(@PathVariable Long reviewId) {
        return R.ok(reviewService.getDetail(reviewId));
    }
}
