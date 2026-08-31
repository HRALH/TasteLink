package com.tastelink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户 VO。username 仅本人场景返回，否则为 null（序列化省略）；password 永不出现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO {

    private Long id;

    /** 仅本人主页或必要场景返回，否则为 null（序列化省略） */
    private String username;

    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer followingCount;
    private Integer followerCount;
    private Integer reviewCount;

    /** 当前登录人是否已关注；未登录为 false */
    private Boolean hasFollowed;
}
