package com.tastelink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页聚合：热门店铺 + 热门点评（各 10 条，可按城市筛选）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeVO {

    private List<ShopVO> hotShops;
    private List<ReviewVO> hotReviews;
}
