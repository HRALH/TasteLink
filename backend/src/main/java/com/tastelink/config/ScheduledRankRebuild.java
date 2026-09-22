package com.tastelink.config;

import com.tastelink.service.HotRankService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 热度排行定时对账（v2 Phase A）：周期性以 MySQL 为准重建全局 Redis ZSet，
 * 修复点赞写缓存的偶发漂移，并兜底冷启动时的空 ZSet。
 * <p>
 * B5 起触发方式二选一：{@code tastelink.xxl-job.enabled=true} 时由调度中心按 admin 侧 cron 触发
 * {@link #rebuildByXxlJob()}，本地 {@code @Scheduled} 空转；为 false（默认）时仍走本地 cron + ShedLock。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledRankRebuild {

    private final HotRankService hotRankService;

    @Value("${tastelink.rank.rebuild-top-size:200}")
    private int rebuildTopSize;

    @Value("${tastelink.xxl-job.enabled:false}")
    private boolean xxlJobEnabled;

    /** 默认每 10 分钟一次；cron 由 tastelink.rank.rebuild-cron 配置。xxl-job 开启时本方法让位。 */
    @Scheduled(cron = "${tastelink.rank.rebuild-cron:0 */10 * * * *}")
    @SchedulerLock(name = "rank-rebuild", lockAtMostFor = "${tastelink.shedlock.rank-rebuild:PT4M}",
            lockAtLeastFor = "PT10S")
    public void rebuild() {
        if (xxlJobEnabled) {
            return;
        }
        hotRankService.rebuild(rebuildTopSize);
    }

    /**
     * XXL-JOB 触发器：handler 名须与 admin 侧任务的 JobHandler 完全一致。
     * <p>勿加 {@code @SchedulerLock}——本地空转仍会短暂持同名锁，可能把本次运行静默跳过（见 XxlJobConfig 类注释）。
     */
    @XxlJob("rankRebuildHandler")
    public void rebuildByXxlJob() {
        XxlJobHelper.log("rank rebuild start: topSize={}", rebuildTopSize);
        hotRankService.rebuild(rebuildTopSize);
        XxlJobHelper.handleSuccess("rank rebuild done");
    }
}
