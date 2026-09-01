package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.request.UpdateProfileRequest;
import com.tastelink.common.PageResult;
import com.tastelink.dto.response.ReviewVO;
import com.tastelink.dto.response.UserVO;
import com.tastelink.service.ReviewService;
import com.tastelink.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户：当前登录人 /me、查看某用户主页。
 * 用户点评列表 GET /users/{userId}/reviews 跨模块,见 ReviewController。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReviewService reviewService;

    @GetMapping("/me")
    public R<UserVO> currentUser() {
        return R.ok(userService.getCurrentUserVO());
    }

    @PutMapping("/me")
    public R<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        return R.ok(userService.updateUserProfile(req));
    }

    @GetMapping("/{userId}")
    public R<UserVO> getUser(@PathVariable Long userId) {
        return R.ok(userService.getUserById(userId));
    }

    @GetMapping("/{userId}/reviews")
    public R<PageResult<ReviewVO>> userReviews(@PathVariable Long userId, PageQuery pq) {
        return R.ok(reviewService.listByUser(userId, pq));
    }
}
