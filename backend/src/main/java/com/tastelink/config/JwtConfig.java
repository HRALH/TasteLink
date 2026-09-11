package com.tastelink.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * JWT 配置属性（tastelink.jwt.*）。
 * <p>
 * B1-3 启动校验：secret 为仓库占位默认值或长度 &lt;32 时，仅 local profile 放行（warn）；
 * 其余 profile 直接拒绝启动（fail-fast），防忘配 JWT_SECRET 导致全员可伪造 token（含 ADMIN）。
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tastelink.jwt")
public class JwtConfig {

    /** 仓库内占位默认值（与 application.yml 的 JWT_SECRET 缺省值一致），仅 local 可用。 */
    public static final String DEFAULT_SECRET =
            "change-me-please-use-a-long-random-secret-string-of-at-least-32-chars";

    private static final int MIN_SECRET_LEN = 32;

    private final Environment environment;

    public JwtConfig(Environment environment) {
        this.environment = environment;
    }

    private String secret = DEFAULT_SECRET;
    private long expireSeconds = 86400L;
    private String header = "Authorization";
    private String prefix = "Bearer ";

    @PostConstruct
    void validateSecret() {
        boolean weak = secret == null || DEFAULT_SECRET.equals(secret) || secret.length() < MIN_SECRET_LEN;
        if (!weak) {
            return;
        }
        boolean local = Arrays.asList(environment.getActiveProfiles()).contains("local");
        if (local) {
            log.warn("JWT secret 为占位默认值或过短（仅 local profile 允许）；部署前请经 JWT_SECRET 注入 ≥{} 位随机密钥",
                    MIN_SECRET_LEN);
        } else {
            throw new IllegalStateException(
                    "JWT secret 为占位默认值或长度不足 " + MIN_SECRET_LEN
                            + "（仅 local profile 允许弱密钥）。请经环境变量 JWT_SECRET 注入强随机密钥后再启动。");
        }
    }
}
