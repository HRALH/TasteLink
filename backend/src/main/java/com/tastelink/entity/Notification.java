package com.tastelink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知（产品优化 F1）。
 * 关系式设计：actor_id 关联 t_user，读时批量回查昵称/头像，避免快照过期（与 ReviewVO 装配同手法）。
 */
@Data
@TableName("t_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者（被互动方） */
    private Long userId;

    /** 触发者（点赞/评论/关注的人） */
    private Long actorId;

    /** 类型：REVIEW_LIKED / REVIEW_COMMENTED / USER_FOLLOWED */
    private String type;

    /** 目标类型：REVIEW / USER */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 快照摘要（如评论内容片段） */
    private String preview;

    /** 0 未读 / 1 已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
