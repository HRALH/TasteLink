package com.tastelink.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布点评请求。图片先经 /files/image 上传拿 URL，再随此提交，上限 9 张。
 */
@Data
public class CreateReviewRequest {

    @NotBlank(message = "点评内容不能为空")
    @Size(max = 2000, message = "点评内容不超过 2000 字")
    private String content;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 5, message = "评分最高 5 分")
    private Integer rating;

    /** 图片可访问 URL 列表（有序），可空 */
    @Size(max = 9, message = "图片数量不超过 9 张")
    private List<String> imageUrls;
}
