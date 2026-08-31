package com.tastelink.service;

import com.tastelink.dto.response.HomeVO;

/**
 * 首页推荐：热门店铺 + 热门点评（计数排序，无独立推荐算法）。
 */
public interface RecommendService {

    /** 首页热门内容，city 为空则取全局热门 */
    HomeVO home(String city);
}
