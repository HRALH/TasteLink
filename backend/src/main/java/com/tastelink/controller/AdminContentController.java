package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.R;
import com.tastelink.dto.response.AdminReviewVO;
import com.tastelink.dto.response.ReportVO;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.AdminReviewService;
import com.tastelink.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台内容治理（产品优化 F4）：下架/恢复点评 + 举报分页查看。
 * 前缀 {@code /api/v1/admin/**} 由 SecurityConfig 统一 hasRole('ADMIN')。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminReviewService adminReviewService;
    private final ReportService reportService;

    /** 分页查所有点评（含已下架），status 可空/1/0，供后台列表下架/恢复操作。 */
    @GetMapping("/reviews")
    public R<PageResult<AdminReviewVO>> listReviews(@RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size,
                                                     @RequestParam(required = false) Integer status) {
        SecurityContextHelper.requireCurrentUserId();
        return R.ok(adminReviewService.listReviews(page, size, status));
    }

    @PutMapping("/reviews/{id}/hide")
    public R<Void> hideReview(@PathVariable Long id) {
        SecurityContextHelper.requireCurrentUserId();
        adminReviewService.hideReview(id);
        return R.ok(null);
    }

    @PutMapping("/reviews/{id}/restore")
    public R<Void> restoreReview(@PathVariable Long id) {
        SecurityContextHelper.requireCurrentUserId();
        adminReviewService.restoreReview(id);
        return R.ok(null);
    }

    /** 举报分页查看（运营据状态下架线索），status 可空/PENDING/RESOLVED */
    @GetMapping("/reports")
    public R<PageResult<ReportVO>> listReports(@RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size,
                                                @RequestParam(required = false) String status) {
        SecurityContextHelper.requireCurrentUserId();
        return R.ok(reportService.listReports(page, size, status));
    }
}
