package com.tastelink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容举报（产品优化 F4）。uk_reporter_target 限同一用户对同一目标只记一条。
 */
@Data
@TableName("t_report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterId;

    /** 目标类型：REVIEW/COMMENT/USER/SHOP（见 Constants.REPORT_TARGET_*） */
    private String targetType;

    private Long targetId;

    /** 举报理由（枚举文案 KEY，由前端传入） */
    private String reason;

    /** PENDING/RESOLVED（见 Constants.REPORT_*） */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
