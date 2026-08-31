package com.tastelink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求（单层，无 parent_id）。
 */
@Data
public class CreateCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不超过 500 字")
    private String content;
}
