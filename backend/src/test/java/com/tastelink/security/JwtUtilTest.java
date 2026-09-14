package com.tastelink.security;

import com.tastelink.config.JwtConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JwtUtil 签发/解析单测：B1-3 起 token 携带 jti（UUID）供吊销黑名单用。
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"local"});
        JwtConfig config = new JwtConfig(env);
        config.setSecret("0123456789abcdef0123456789abcdef-fedcba98");
        config.setExpireSeconds(3600);
        jwtUtil = new JwtUtil(config);
        jwtUtil.init();
    }

    @Test
    void generate_thenParse_roundTripsClaimsIncludingJti() {
        String token = jwtUtil.generate(7L, "tom", "USER");

        Claims claims = jwtUtil.parse(token);
        assertEquals("tom", claims.getSubject());
        assertEquals("7", claims.get("userId", String.class));
        assertEquals("USER", claims.get("role", String.class));
        // B1-3：jti 存在且为 UUID 形态
        assertNotNull(claims.getId());
        assertTrue(claims.getId().matches("[0-9a-f-]{36}"), "jti 应为 UUID");
    }

    @Test
    void generate_producesDistinctJtiPerToken() {
        String t1 = jwtUtil.generate(7L, "tom", "USER");
        String t2 = jwtUtil.generate(7L, "tom", "USER");
        // 同用户连发两枚 token 也各自独立可吊销
        assertTrue(!jwtUtil.parse(t1).getId().equals(jwtUtil.parse(t2).getId()));
    }
}
