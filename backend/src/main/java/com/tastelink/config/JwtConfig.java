package com.tastelink.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性（tastelink.jwt.*）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tastelink.jwt")
public class JwtConfig {

    private String secret = "change-me-please-use-a-long-random-secret-string-of-at-least-32-chars";
    private long expireSeconds = 86400L;
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
