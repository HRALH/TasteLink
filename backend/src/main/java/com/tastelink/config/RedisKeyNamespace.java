package com.tastelink.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Redis key 环境命名空间（B4-4）：所有业务 key 统一带 {@code tastelink:{profile}:} 前缀，
 * 防多环境共用同一 Redis 实例时串数据。使用者：热度 ZSet（rank）、登录限流、JWT 黑名单。
 */
@Component
public class RedisKeyNamespace {

    private final String prefix;

    public RedisKeyNamespace(Environment env) {
        String[] profiles = env.getActiveProfiles();
        String profile = profiles.length > 0 ? String.join("-", profiles) : "default";
        this.prefix = "tastelink:" + profile + ":";
    }

    /** 拼装完整 key：{@code tastelink:{profile}:{suffix}}。 */
    public String key(String suffix) {
        return prefix + suffix;
    }
}
