package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.CategoryVO;
import com.tastelink.dto.response.ShopDetailVO;
import com.tastelink.dto.response.ShopVO;
import com.tastelink.dto.response.ReviewVO;
import com.tastelink.entity.Shop;
import com.tastelink.entity.ShopCategory;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.ShopCategoryMapper;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.ReviewService;
import com.tastelink.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final ShopCategoryMapper categoryMapper;
    private final ReviewService reviewService;

    @Override
    public PageResult<ShopVO> listShops(String keyword, Long categoryId, String city, String sortBy, PageQuery pq) {
        Page<Shop> page = new Page<>(pq.getPageOrDefault(), pq.getSizeOrDefault());
        LambdaQueryWrapper<Shop> w = new LambdaQueryWrapper<>();
        w.eq(Shop::getStatus, Constants.STATUS_NORMAL);
        if (StringUtils.hasText(keyword)) {
            w.like(Shop::getName, keyword);
        }
        if (categoryId != null) {
            w.eq(Shop::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(city)) {
            w.eq(Shop::getCity, city);
        }
        if ("rating".equalsIgnoreCase(sortBy)) {
            w.orderByDesc(Shop::getAvgRating);
        } else {
            // 默认热度：点评数倒序，点赞数次级
            w.orderByDesc(Shop::getReviewCount).orderByDesc(Shop::getLikeCount);
        }
        shopMapper.selectPage(page, w);

        List<Shop> shops = page.getRecords();
        Map<Long, String> nameMap = categoryNameMap(shops);
        List<ShopVO> vos = shops.stream()
                .map(s -> toShopVO(s, nameMap.get(s.getCategoryId())))
                .toList();
        return PageResult.from(page, vos);
    }

    @Override
    public ShopDetailVO getShopDetail(Long shopId) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getId, shopId)
                .eq(Shop::getStatus, Constants.STATUS_NORMAL));
        if (shop == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }
        ShopDetailVO vo = new ShopDetailVO();
        vo.setId(shop.getId());
        vo.setName(shop.getName());
        vo.setCategoryId(shop.getCategoryId());
        vo.setCategoryName(categoryNameSingle(shop.getCategoryId()));
        vo.setCity(shop.getCity());
        vo.setAddress(shop.getAddress());
        vo.setCoverUrl(shop.getCoverUrl());
        vo.setAvgRating(shop.getAvgRating());
        vo.setReviewCount(shop.getReviewCount());
        vo.setPhone(shop.getPhone());
        vo.setDescription(shop.getDescription());
        vo.setLikeCount(shop.getLikeCount());
        vo.setTopReviews(reviewService.recentByShop(shopId, 3));
        return vo;
    }

    @Override
    public List<CategoryVO> listCategories() {
        List<ShopCategory> cs = categoryMapper.selectList(new LambdaQueryWrapper<ShopCategory>()
                .orderByAsc(ShopCategory::getSortOrder));
        return cs.stream()
                .map(c -> CategoryVO.builder()
                        .id(c.getId())
                        .code(c.getCode())
                        .name(c.getName())
                        .iconUrl(c.getIconUrl())
                        .sortOrder(c.getSortOrder())
                        .build())
                .toList();
    }

    @Override
    public List<ShopVO> hotShops(String city, int limit) {
        Page<Shop> page = new Page<>(1, limit, false);
        LambdaQueryWrapper<Shop> w = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getStatus, Constants.STATUS_NORMAL);
        if (StringUtils.hasText(city)) {
            w.eq(Shop::getCity, city);
        }
        w.orderByDesc(Shop::getReviewCount).orderByDesc(Shop::getLikeCount);
        shopMapper.selectPage(page, w);
        Map<Long, String> nameMap = categoryNameMap(page.getRecords());
        return page.getRecords().stream()
                .map(s -> toShopVO(s, nameMap.get(s.getCategoryId())))
                .toList();
    }

    private ShopVO toShopVO(Shop s, String categoryName) {
        return ShopVO.builder()
                .id(s.getId())
                .name(s.getName())
                .categoryId(s.getCategoryId())
                .categoryName(categoryName)
                .city(s.getCity())
                .address(s.getAddress())
                .coverUrl(s.getCoverUrl())
                .avgRating(s.getAvgRating())
                .reviewCount(s.getReviewCount())
                .build();
    }

    private Map<Long, String> categoryNameMap(List<Shop> shops) {
        Set<Long> ids = shops.stream()
                .map(Shop::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ShopCategory> cs = categoryMapper.selectBatchIds(ids);
        return cs.stream().collect(Collectors.toMap(ShopCategory::getId, ShopCategory::getName));
    }

    private String categoryNameSingle(Long id) {
        if (id == null) {
            return null;
        }
        ShopCategory c = categoryMapper.selectById(id);
        return c == null ? null : c.getName();
    }
}
