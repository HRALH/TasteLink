package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.request.LoginRequest;
import com.tastelink.dto.request.RegisterRequest;
import com.tastelink.dto.response.LoginVO;
import com.tastelink.dto.response.RegisterVO;
import com.tastelink.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证：注册 / 登录。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public R<RegisterVO> register(@Valid @RequestBody RegisterRequest req) {
        return R.ok(new RegisterVO(userService.register(req), req.getUsername()));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(userService.login(req));
    }
}
