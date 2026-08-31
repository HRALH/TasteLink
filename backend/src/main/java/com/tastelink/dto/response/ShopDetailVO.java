package com.tastelink.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 店铺详情：继承 ShopVO，附加联系/简介/点赞累计/近期点评预览。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShopDetailVO extends ShopVO {

    private String phone;
    private String description;
    private Integer likeCount;
    /** 近期点评预览（默认 3 条） */
    private List<ReviewVO> topReviews;
}
