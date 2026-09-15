package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.request.CreateReportRequest;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报（产品优化 F4）：登录用户提交内容举报。
 * 需登录（不在白名单，走 anyRequest().authenticated()）。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public R<Void> report(@Valid @RequestBody CreateReportRequest req) {
        Long me = SecurityContextHelper.requireCurrentUserId();
        reportService.report(me, req);
        return R.ok(null);
    }
}
