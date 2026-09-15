package com.tastelink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报请求（产品优化 F4）。
 */
@Data
public class CreateReportRequest {

    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    @NotNull(message = "举报对象 ID 不能为空")
    private Long targetId;

    @NotBlank(message = "举报理由不能为空")
    @Size(max = 255, message = "举报理由不超过 255 字")
    private String reason;
}
