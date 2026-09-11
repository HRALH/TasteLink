package com.tastelink.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JwtConfig 启动校验单测（B1-3 fail-fast）。
 * 非 local profile + 弱密钥 → 拒绝启动；local → 仅 warn 放行；强密钥任意 profile 放行。
 */
class JwtConfigTest {

    private JwtConfig configWithProfiles(String... profiles) {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(profiles);
        return new JwtConfig(env);
    }

    @Test
    void validateSecret_localProfileWithDefaultSecret_passes() {
        JwtConfig cfg = configWithProfiles("local");
        cfg.setSecret(JwtConfig.DEFAULT_SECRET);
        assertDoesNotThrow(cfg::validateSecret);
    }

    @Test
    void validateSecret_prodProfileWithDefaultSecret_failsFast() {
        JwtConfig cfg = configWithProfiles("prod");
        cfg.setSecret(JwtConfig.DEFAULT_SECRET);
        assertThrows(IllegalStateException.class, cfg::validateSecret);
    }

    @Test
    void validateSecret_prodProfileWithShortSecret_failsFast() {
        JwtConfig cfg = configWithProfiles("prod");
        cfg.setSecret("too-short-secret");
        assertThrows(IllegalStateException.class, cfg::validateSecret);
    }

    @Test
    void validateSecret_noActiveProfileWithDefaultSecret_failsFast() {
        // 无激活 profile 视为非 local（防御缺配 SPRING_PROFILES_ACTIVE 的部署）
        JwtConfig cfg = configWithProfiles();
        cfg.setSecret(JwtConfig.DEFAULT_SECRET);
        assertThrows(IllegalStateException.class, cfg::validateSecret);
    }

    @Test
    void validateSecret_strongSecret_passesOnAnyProfile() {
        JwtConfig cfg = configWithProfiles("prod");
        cfg.setSecret("0123456789abcdef0123456789abcdef-fedcba98");
        assertDoesNotThrow(cfg::validateSecret);
    }
}
