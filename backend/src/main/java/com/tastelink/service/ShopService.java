package com.tastelink.service;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.CategoryVO;
import com.tastelink.dto.response.ShopDetailVO;
import com.tastelink.dto.response.ShopVO;

import java.util.List;

public interface ShopService {

    /** 店铺列表（搜索/分类/城市筛选 + 排序） */
    PageResult<ShopVO> listShops(String keyword, Long categoryId, String city, String sortBy, PageQuery pq);

    /** 店铺详情（含近 3 条点评预览） */
    ShopDetailVO getShopDetail(Long shopId);

    /** 分类字典（按 sort_order 升序） */
    List<CategoryVO> listCategories();

    /** 首页热门店铺（按 review_count 倒序，再按 like_count 倒序，city 可选） */
    List<ShopVO> hotShops(String city, int limit);

    /**
     * 店铺 id 序 → ShopVO，<b>按入参 id 顺序还原</b>（ES 命中后 ShopVO 组装用，v2 Phase D）。
     * 仅返回 status=NORMAL 的店铺，已删的 id 会被跳过。
     */
    List<ShopVO> toVOsByIds(List<Long> orderedIds);
}
