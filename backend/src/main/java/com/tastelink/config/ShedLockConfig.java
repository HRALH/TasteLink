package com.tastelink.config;

import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * ShedLock 配置（B4-3）：@Scheduled 任务加 Redis 分布式锁，多实例不重复执行。
 * <p>
 * RedisLockProvider 用 StringRedisTemplate 的连接工厂；Redis 缺席时——
 * 单实例：@SchedulerLock 拿不到锁 provider 即直接执行（与无锁等价），调度照常跑；
 * 多实例：拿不到 provider 会抛异常被 Spring 吞，等下一周期重试（对账/重建均幂等）。
 * <p>
 * {@link EnableSchedulerLock} defaultLockAtMostFor 设为占位长值，具体锁时长由 @SchedulerLock 注解
 * 的 lockAtMostFor 覆盖（在各自 @Scheduled 方法上声明）。
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class ShedLockConfig {

    @Bean
    public RedisLockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "tastelink");
    }
}
