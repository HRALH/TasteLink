package com.tastelink.service;

import com.tastelink.common.PageResult;
import com.tastelink.dto.response.AdminReviewVO;

/**
 * 后台点评治理（产品优化 F4）：下架/恢复点评，镜像店铺/用户计数 + 热度 zrem。
 */
public interface AdminReviewService {

    /** 下架点评（status=0），同事务回扣 shop/user 计数并重算 avg；afterCommit 热度 zrem。 */
    void hideReview(Long reviewId);

    /** 恢复点评（status=1），同事务加回 shop/user 计数并重算 avg。 */
    void restoreReview(Long reviewId);

    /** 分页查所有点评（含已下架），status 可空/1/0；供后台列表下架/恢复操作。 */
    PageResult<AdminReviewVO> listReviews(Integer page, Integer size, Integer status);
}
