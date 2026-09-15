package com.tastelink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 举报 VO（后台查看用，含举报人昵称）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportVO {

    private Long id;
    private Long reporterId;
    private String reporterNickname;
    private String targetType;
    private Long targetId;
    private String reason;
    /** PENDING/RESOLVED */
    private String status;
    /** 举报时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
