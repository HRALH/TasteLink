package com.tastelink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属店铺ID（逻辑外键 -> t_shop.id） */
    private Long shopId;

    /** 发布用户ID（逻辑外键 -> t_user.id） */
    private Long userId;

    /** 冗余店铺城市（首页按城市精选点评，免 JOIN） */
    private String city;

    private String content;

    /** 评分 1-5 */
    private Integer rating;

    /** 点赞数（冗余） */
    private Integer likeCount;

    /** 评论数（冗余） */
    private Integer replyCount;

    /** 状态：1 正常 / 0 隐藏 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
