package com.tastelink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_review_like")
public class ReviewLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reviewId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
