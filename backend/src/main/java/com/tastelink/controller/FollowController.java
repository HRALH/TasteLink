package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.FollowCountVO;
import com.tastelink.common.PageResult;
import com.tastelink.dto.response.UserVO;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注：关注/取关（幂等）、关注列表/粉丝列表。
 * 与 UserController 共享 /users 前缀，按子路径区分（follow / followings / followers）。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    public R<FollowCountVO> follow(@PathVariable Long userId) {
        Long me = SecurityContextHelper.requireCurrentUserId();
        return R.ok(followService.follow(userId, me));
    }

    @DeleteMapping("/{userId}/follow")
    public R<FollowCountVO> unfollow(@PathVariable Long userId) {
        Long me = SecurityContextHelper.requireCurrentUserId();
        return R.ok(followService.unfollow(userId, me));
    }

    @GetMapping("/{userId}/followings")
    public R<PageResult<UserVO>> followings(@PathVariable Long userId, PageQuery pq) {
        return R.ok(followService.listFollowings(userId, pq));
    }

    @GetMapping("/{userId}/followers")
    public R<PageResult<UserVO>> followers(@PathVariable Long userId, PageQuery pq) {
        return R.ok(followService.listFollowers(userId, pq));
    }
}
