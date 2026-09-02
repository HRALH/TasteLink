package com.tastelink.consumer;

import com.tastelink.config.RabbitMQConfig;
import com.tastelink.dto.mq.ShopCleanupMessage;
import com.tastelink.service.ShopCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 店铺延时清理消息消费者（v2 Phase C）。
 * <p>
 * 监听 {@link RabbitMQConfig#CLEANUP_QUEUE}：delay-queue TTL 到期后经 cleanupExchange 转入。
 * 消费失败触发 Spring AMQP 重试（{@code spring.rabbitmq.listener.simple.retry.*}），耗尽后
 * reject(no-requeue) → 由 cleanup-queue 的 {@code x-dead-letter-exchange} 路由进 DLQ 兜底毒消息。
 * {@code ShopCleanupService#cleanup} 本身幂等，重复投递安全。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopCleanupConsumer {

    private final ShopCleanupService shopCleanupService;

    @RabbitListener(queues = RabbitMQConfig.CLEANUP_QUEUE)
    public void onShopCleanup(ShopCleanupMessage message) {
        if (message == null || message.getShopId() == null) {
            log.warn("shop cleanup message ignored: invalid payload {}", message);
            return;
        }
        log.info("shop cleanup message received: shopId={}, retry={}", message.getShopId(), message.getRetry());
        // 抛异常触发 retry→DLQ；正常路径幂等清理
        shopCleanupService.cleanup(message.getShopId());
    }
}
