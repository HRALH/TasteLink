package com.tastelink.service;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.CreateReviewRequest;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.ReviewVO;

import java.util.List;

/**
 * 点评服务：详情、店铺/用户点评列表、近期预览、发布。
 * 实现见 ReviewServiceImpl。
 */
public interface ReviewService {

    /** GET /reviews/{reviewId} 点评详情 */
    ReviewVO getDetail(Long reviewId);

    /** GET /shops/{shopId}/reviews 店铺下点评列表（sortBy: time/like） */
    PageResult<ReviewVO> listByShop(Long shopId, PageQuery pq, String sortBy);

    /** GET /users/{userId}/reviews 某用户发布的点评列表 */
    PageResult<ReviewVO> listByUser(Long userId, PageQuery pq);

    /** 店铺详情「近期点评」预览，按 like_count desc 取前 limit 条 */
    List<ReviewVO> recentByShop(Long shopId, int limit);

    /** POST /shops/{shopId}/reviews 发布点评（同事务维护店铺/用户计数） */
    ReviewVO createReview(Long shopId, CreateReviewRequest req, Long userId);

    /** 首页热门点评（按 like_count 倒序，再按 reply_count 倒序，city 可选） */
    List<ReviewVO> hotReviews(String city, int limit);
}
