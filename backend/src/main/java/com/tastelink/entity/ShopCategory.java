package com.tastelink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_shop_category")
public class ShopCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类编码：HOTPOT/COFFEE/BBQ/JAPANESE ... */
    private String code;

    /** 分类中文名 */
    private String name;

    /** 展示排序，越小越靠前 */
    private Integer sortOrder;

    private String iconUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
