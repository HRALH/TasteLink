package com.tastelink.security;

import com.tastelink.config.JwtConfig;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 鉴权过滤器：解析 Authorization: Bearer，校验通过写入 SecurityContext。
 * 白名单请求即便无 token，也安全放行（由 SecurityFilterChain 的授权规则决定）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtUtil.parse(token);
                if (jwtBlacklistService.isRevoked(claims.getId())) {
                    // B1-3：登出/吊销名单命中 → 按未登录处理（受保护接口由入口渲染 401）
                    SecurityContextHolder.clearContext();
                } else {
                    Long userId = Long.valueOf(claims.get("userId", String.class));
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);
                    LoginUser principal = new LoginUser(userId, username, role);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // token 非法/过期：清空上下文，按未登录处理（公开接口仍可放行，受保护接口由入口返回 401）
                // B4-2：补 debug 日志，便于排查 401 归因（token 过期 vs 被篡改 vs 黑名单命中）
                log.debug("jwt parse failed, treat as anonymous: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtConfig.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(jwtConfig.getPrefix())) {
            return header.substring(jwtConfig.getPrefix().length()).trim();
        }
        return null;
    }
}
