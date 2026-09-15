package com.tastelink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知 VO（产品优化 F1）。actor 昵称/头像读时批量回查填充。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationVO {

    private Long id;
    /** 类型：REVIEW_LIKED / REVIEW_COMMENTED / USER_FOLLOWED */
    private String type;
    private Long actorId;
    private String actorNickname;
    private String actorAvatarUrl;
    /** 目标类型：REVIEW / USER */
    private String targetType;
    private Long targetId;
    /** 摘要（评论内容片段等） */
    private String preview;
    /** 是否已读 */
    private Boolean isRead;
    /** 发生时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
