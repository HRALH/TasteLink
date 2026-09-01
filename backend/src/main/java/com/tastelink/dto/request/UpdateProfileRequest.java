package com.tastelink.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改资料请求：字段均可选，非空才更新；username 不可改（接口不暴露此字段）。
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 64, message = "昵称长度不超过 64")
    private String nickname;

    @Size(max = 512, message = "头像 URL 长度不超过 512")
    private String avatarUrl;

    @Size(max = 255, message = "简介长度不超过 255")
    private String bio;
}
