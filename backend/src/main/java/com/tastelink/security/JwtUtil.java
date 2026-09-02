package com.tastelink.security;

import com.tastelink.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发与解析（jjwt 0.12.x）。
 * Claims: subject=username, 自定义 userId / role（v2 Phase B 起带 role，鉴权依此 hasRole）。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;
    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtConfig.getExpireSeconds());
        return Jwts.builder()
                .subject(username)
                .claim("userId", Long.toString(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpireSeconds() {
        return jwtConfig.getExpireSeconds();
    }
}
