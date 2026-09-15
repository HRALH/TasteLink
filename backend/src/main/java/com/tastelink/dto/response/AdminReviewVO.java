package com.tastelink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 后台点评治理列表项（含 status，区分正常/已下架；区别于公开 ReviewVO 不含 status）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminReviewVO {

    private Long id;
    private Long shopId;
    private String shopName;
    private Long userId;
    private String userNickname;
    private String content;
    private Integer rating;
    private Integer likeCount;
    private Integer replyCount;
    /** 1 正常 / 0 已下架 */
    private Integer status;
    /** 发表时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
