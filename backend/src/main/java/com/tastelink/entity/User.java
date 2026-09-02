package com.tastelink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码（BCrypt 哈希），任何 VO 不得返回 */
    private String password;

    private String nickname;

    private String avatarUrl;

    private String bio;

    /** 关注数（冗余） */
    private Integer followingCount;

    /** 粉丝数（冗余） */
    private Integer followerCount;

    /** 发布点评数（冗余） */
    private Integer reviewCount;

    /** 角色：USER 普通 / ADMIN 管理员（v2 Phase B） */
    private String role;

    /** 状态：1 正常 / 0 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
