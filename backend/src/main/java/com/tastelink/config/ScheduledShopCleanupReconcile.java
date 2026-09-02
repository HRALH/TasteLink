package com.tastelink.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tastelink.common.Constants;
import com.tastelink.entity.Shop;
import com.tastelink.mapper.ShopMapper;
import com.tastelink.service.ShopCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /** 默认每 2 分钟；cron 由 tastelink.rabbitmq.reconcile-cron 配置。 */
    @Scheduled(cron = "${tastelink.rabbitmq.reconcile-cron:0 */2 * * * *}")
    public void reconcile() {
        // 仅认已超「延时窗口 + 宽限」仍滞留被标记者，否则仍在 MQ 正常清理途中，不抢跑
        LocalDateTime cutoff = LocalDateTime.now().minus(delayMs + graceMs, ChronoUnit.MILLIS);
        List<Shop> lingering = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getStatus, Constants.STATUS_HIDDEN)
                .lt(Shop::getUpdateTime, cutoff));
        if (lingering.isEmpty()) {
            return;
        }
        log.info("shop cleanup reconcile: {} lingering shop(s)", lingering.size());
        for (Shop s : lingering) {
            try {
                shopCleanupService.cleanup(s.getId());
            } catch (Exception e) {
                log.warn("shop cleanup reconcile failed for shopId={}: {}", s.getId(), e.getMessage());
            }
        }
    }
}
