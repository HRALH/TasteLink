package com.tastelink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TasteLink 后端启动类。
 * 统一扫描 Mapper 接口包；其余组件默认扫描 com.tastelink 下。
 * @EnableScheduling 启用 v2 Phase A 的热度对账定时任务（{@code com.tastelink.config.ScheduledRankRebuild}）。
 */
@SpringBootApplication
@MapperScan("com.tastelink.mapper")
@EnableScheduling
public class TasteLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(TasteLinkApplication.class, args);
    }
}
