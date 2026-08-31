package com.tastelink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    private Long id;
    private Long reviewId;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private String content;
    /** 评论时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
