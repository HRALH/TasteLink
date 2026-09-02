package com.tastelink.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员编辑店铺请求（v2 Phase B）：字段均可选，非空/null 才更新（沿用 {@link UpdateProfileRequest} 口径）。
 * 并发控制不在此 DTO 暴露 version——服务端按 {@code @Version} 自动处理，冲突返回 409。
 */
@Data
public class UpdateShopRequest {

    @Size(max = 128, message = "店铺名称长度不超过 128")
    private String name;

    /** 店铺分类ID；非空则校验存在性后再更新 */
    private Long categoryId;

    @Size(max = 64, message = "城市长度不超过 64")
    private String city;

    @Size(max = 255, message = "地址长度不超过 255")
    private String address;

    @Size(max = 32, message = "电话长度不超过 32")
    private String phone;

    @Size(max = 512, message = "封面 URL 长度不超过 512")
    private String coverUrl;

    @Size(max = 1000, message = "简介长度不超过 1000")
    private String description;
}
