package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.R;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.ReviewVO;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.FollowService;
import com.tastelink.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注 feed（产品优化 F3）：补全"关注→看见 TA 的点评"闭环。
 * 拉取式实现：取关注者 id 列表 → 点评按时间倒序分页，复用 ReviewService 装配。
 * 需登录（不在白名单，走 anyRequest().authenticated()）。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FollowService followService;
    private final ReviewService reviewService;

    @GetMapping("/following")
    public R<PageResult<ReviewVO>> following(PageQuery pq) {
        Long me = SecurityContextHelper.requireCurrentUserId();
        return R.ok(reviewService.listByFollowees(followService.followeeIds(me), pq));
    }
}
