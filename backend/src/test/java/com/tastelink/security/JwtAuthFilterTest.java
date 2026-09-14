package com.tastelink.security;

import com.tastelink.config.JwtConfig;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JwtAuthFilter 单测（B1-3 黑名单命中 401 语义）。
 * 黑名单命中 → 不写 SecurityContext（受保护接口由入口渲染 401）；正常 token → 写入登录态。
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtConfig jwtConfig;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private JwtBlacklistService jwtBlacklistService;
    @Mock
    private FilterChain chain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtConfig, jwtUtil, jwtBlacklistService);
        when(jwtConfig.getHeader()).thenReturn("Authorization");
        when(jwtConfig.getPrefix()).thenReturn("Bearer ");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    private Claims claimsOf(String jti) {
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn(jti);
        // lenient：黑名单命中分支在读 userId/subject/role 前即清空上下文，不触达以下桩
        org.mockito.Mockito.lenient().when(claims.get("userId", String.class)).thenReturn("7");
        org.mockito.Mockito.lenient().when(claims.getSubject()).thenReturn("tom");
        org.mockito.Mockito.lenient().when(claims.get("role", String.class)).thenReturn("USER");
        return claims;
    }

    @Test
    void blacklistedToken_leavesContextEmpty() throws Exception {
        MockHttpServletRequest req = requestWithToken("tk");
        Claims claims = claimsOf("jti-bad");
        when(jwtUtil.parse("tk")).thenReturn(claims);
        when(jwtBlacklistService.isRevoked("jti-bad")).thenReturn(true);

        filter.doFilterInternal(req, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "黑名单命中不得写入登录态");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void validToken_setsAuthentication() throws Exception {
        MockHttpServletRequest req = requestWithToken("tk");
        Claims claims = claimsOf("jti-ok");
        when(jwtUtil.parse("tk")).thenReturn(claims);
        when(jwtBlacklistService.isRevoked("jti-ok")).thenReturn(false);

        filter.doFilterInternal(req, new MockHttpServletResponse(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void legacyTokenWithoutJti_stillPasses() throws Exception {
        // 旧 token（无 jti claim，B1-3 前签发）在有效期内仍可用；登出时无可吊销
        MockHttpServletRequest req = requestWithToken("tk");
        Claims claims = claimsOf(null);
        when(jwtUtil.parse("tk")).thenReturn(claims);

        filter.doFilterInternal(req, new MockHttpServletResponse(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
