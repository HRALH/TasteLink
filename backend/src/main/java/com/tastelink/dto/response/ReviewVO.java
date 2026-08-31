package com.tastelink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 点评 VO（含图片列表、作者信息、点赞数、评论数；登录态含 hasLiked）。
 * shopName 在店铺下点评列表场景为 null（序列化省略），首页/用户点评列表场景填充。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewVO {

    private Long id;
    private Long shopId;
    /** 首页 / 用户点评列表场景填充，店铺下点评列表可省略 */
    private String shopName;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private String content;
    private Integer rating;
    private Integer likeCount;
    private Integer replyCount;
    /** 图片 URL 列表（有序） */
    private List<String> images;
    /** 当前登录人是否已点赞；未登录为 false */
    private Boolean hasLiked;
    /** 发表时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
