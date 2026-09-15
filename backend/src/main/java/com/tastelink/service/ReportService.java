package com.tastelink.service;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.CreateReportRequest;
import com.tastelink.dto.response.ReportVO;

/**
 * 内容举报（产品优化 F4）：用户举报 + 后台分页查看。
 */
public interface ReportService {

    /** 提交举报（幂等：同一人对同一目标重复举报抛 ALREADY_REPORTED） */
    void report(Long reporterId, CreateReportRequest req);

    /** 后台分页查看举报，status 可空（全部）/PENDING/RESOLVED */
    PageResult<ReportVO> listReports(Integer page, Integer size, String status);
}
