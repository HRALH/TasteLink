package com.tastelink.config;

import com.tastelink.service.HotRankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 热度排行定时对账（v2 Phase A）：周期性以 MySQL 为准重建全局 Redis ZSet，
 * 修复点赞写缓存的偶发漂移，并兜底冷启动时的空 ZSet。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledRankRebuild {

    private final HotRankService hotRankService;

    @Value("${tastelink.rank.rebuild-top-size:200}")
    private int rebuildTopSize;

    /** 默认每 10 分钟一次；cron 由 tastelink.rank.rebuild-cron 配置。 */
    @Scheduled(cron = "${tastelink.rank.rebuild-cron:0 */10 * * * *}")
    @SchedulerLock(name = "rank-rebuild", lockAtMostFor = "${tastelink.shedlock.rank-rebuild:PT4M}",
            lockAtLeastFor = "PT10S")
    public void rebuild() {
        hotRankService.rebuild(rebuildTopSize);
    }
}
