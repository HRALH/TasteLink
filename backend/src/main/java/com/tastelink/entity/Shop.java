package com.tastelink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_shop")
public class Shop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 店铺分类ID（逻辑外键 -> t_shop_category.id） */
    private Long categoryId;

    private String city;

    private String address;

    /** 经度（MVP 预留，不参与筛选） */
    private BigDecimal longitude;

    /** 纬度（MVP 预留，不参与筛选） */
    private BigDecimal latitude;

    private String phone;

    private String coverUrl;

    private String description;

    /** 评分总和（冗余，算 avg 用） */
    private Integer ratingSum;

    /** 平均评分（冗余，0.00-5.00） */
    private BigDecimal avgRating;

    /** 点评数（冗余，首页热度依据） */
    private Integer reviewCount;

    /** 本店点评累计点赞数（冗余，热度加权用） */
    private Integer likeCount;

    /** 状态：1 正常 / 0 下架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
