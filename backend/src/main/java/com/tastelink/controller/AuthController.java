package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.config.JwtConfig;
import com.tastelink.dto.request.LoginRequest;
import com.tastelink.dto.request.RegisterRequest;
import com.tastelink.dto.response.LoginVO;
import com.tastelink.dto.response.RegisterVO;
import com.tastelink.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证：注册 / 登录 / 登出。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtConfig jwtConfig;

    @PostMapping("/register")
    public R<RegisterVO> register(@Valid @RequestBody RegisterRequest req) {
        return R.ok(new RegisterVO(userService.register(req), req.getUsername()));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(userService.login(req));
    }

    /**
     * 登出（B1-3，需登录——未带 token 由安全链 401）：把当前 token 的 jti 写入黑名单（剩余 TTL）。
     * Redis 缺席时静默成功（降级为仅前端清态），前端 best-effort 调用后清本地态即可。
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        userService.logout(resolveToken(request));
        return R.ok(null);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtConfig.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(jwtConfig.getPrefix())) {
            return header.substring(jwtConfig.getPrefix().length()).trim();
        }
        return null;
    }
}
