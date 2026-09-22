package com.tastelink.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tastelink.common.Constants;
import com.tastelink.entity.Shop;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.ShopCleanupService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 店铺延时清理对账（v2 Phase C）。
 * <p>
 * 与 Phase A {@code ScheduledRankRebuild} 同范式：扫描已被标记下架（{@code status=0}）但尚未被 MQ 消费者
 * 物理清理、且已超过「延时窗口+宽限」的滞留店，直接调 {@link ShopCleanupService#cleanup} 补清。
 * 这是删店 at-least-once 的总兜底——MQ 端到端正常时消费者早已把店铺行删掉（此处 selectList 取不到），
 * 仅在 broker 失联/消息丢失时此处生效。{@link com.tastelink.service.ShopCleanupService#cleanup} 幂等，故重复安全。
 * <p>
 * B5 起触发方式二选一：{@code tastelink.xxl-job.enabled=true} 时由调度中心按 admin 侧 cron 触发
 * {@link #reconcileByXxlJob()}，本地 {@code @Scheduled} 空转；为 false（默认）时仍走本地 cron + ShedLock。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledShopCleanupReconcile {

    private final ShopMapper shopMapper;
    private final ShopCleanupService shopCleanupService;

    @Value("${tastelink.rabbitmq.cleanup-delay-ms:5000}")
    private long delayMs;

    @Value("${tastelink.rabbitmq.reconcile-grace-ms:30000}")
    private long graceMs;

    @Value("${tastelink.xxl-job.enabled:false}")
    private boolean xxlJobEnabled;

    /** 默认每 2 分钟；cron 由 tastelink.rabbitmq.reconcile-cron 配置。xxl-job 开启时本方法让位。 */
    @Scheduled(cron = "${tastelink.rabbitmq.reconcile-cron:0 */2 * * * *}")
    @SchedulerLock(name = "shop-cleanup-reconcile", lockAtMostFor = "${tastelink.shedlock.shop-cleanup-reconcile:PT4M}",
            lockAtLeastFor = "PT5S")
    public void reconcile() {
        if (xxlJobEnabled) {
            return;
        }
        doReconcile();
    }

    /**
     * XXL-JOB 触发器：handler 名须与 admin 侧任务的 JobHandler 完全一致。
     * <p>勿加 {@code @SchedulerLock}——本地空转仍会短暂持同名锁，可能把本次运行静默跳过（见 XxlJobConfig 类注释）。
     */
    @XxlJob("shopCleanupReconcileHandler")
    public void reconcileByXxlJob() {
        int cleaned = doReconcile();
        XxlJobHelper.handleSuccess("lingering shops cleaned: " + cleaned);
    }

    /**
     * 扫描并补清滞留店，返回本轮补清的店铺数。
     * <p>
     * 单店失败只记 warn 不中断整轮（下一周期重扫），故此处不向调用方抛异常——XXL-JOB 侧因此
     * 恒为成功，失败详情看应用日志 warn（与 Phase A/C/D 既有「吞异常 + 周期重试」可靠模型一致）。
     */
    private int doReconcile() {
        // 仅认已超「延时窗口 + 宽限」仍滞留被标记者，否则仍在 MQ 正常清理途中，不抢跑
        LocalDateTime cutoff = LocalDateTime.now().minus(delayMs + graceMs, ChronoUnit.MILLIS);
        List<Shop> lingering = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getStatus, Constants.STATUS_HIDDEN)
                .lt(Shop::getUpdateTime, cutoff));
        if (lingering.isEmpty()) {
            return 0;
        }
        log.info("shop cleanup reconcile: {} lingering shop(s)", lingering.size());
        int cleaned = 0;
        for (Shop s : lingering) {
            try {
                shopCleanupService.cleanup(s.getId());
                cleaned++;
            } catch (Exception e) {
                log.warn("shop cleanup reconcile failed for shopId={}: {}", s.getId(), e.getMessage());
            }
        }
        return cleaned;
    }
}
