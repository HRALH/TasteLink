package com.tastelink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_follow")
public class Follow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注者（发起关注的用户ID） */
    private Long followerId;

    /** 被关注者（被关注的用户ID） */
    private Long followeeId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
