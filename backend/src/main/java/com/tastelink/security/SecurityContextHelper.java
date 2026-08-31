package com.tastelink.security;

import com.tastelink.common.ResultCode;
import com.tastelink.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户工具。登录态由 JwtAuthFilter 写入 SecurityContext。
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {
    }

    public static LoginUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) auth.getPrincipal();
    }

    public static Long getCurrentUserId() {
        LoginUser user = getCurrentUser();
        return user == null ? null : user.getUserId();
    }

    /** 强制要求登录态，缺失则抛 401 业务异常。 */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
