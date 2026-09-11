package com.tastelink.service;

import com.tastelink.config.RabbitMQConfig;
import com.tastelink.dto.mq.ShopCleanupMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 店铺延时清理消息生产者（v2 Phase C）。
 * <p>
 * 在 {@link com.tastelink.service.AdminShopService#deleteShop} 标记事务提交后（afterCommit）投递，
 * 令物理级联清理由异步消费者承担、带「撤销误删」延时窗口。broker 不可达时吞异常并记 warn——
 * 由 {@code ScheduledShopCleanupReconcile} 对账补投递，绝不把缓存/MQ 故障抛给删店主流程
 * （与 Phase A {@code HotRankService} 吞 Redis 异常靠 rebuild 兜底同一范式）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopCleanupProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${tastelink.rabbitmq.enabled:true}")
    private boolean enabled;

    /** 投递延时清理消息；{@code enabled=false} 或 broker 不可达均不抛，靠对账兜底。 */
    public void send(Long shopId) {
        if (!enabled || shopId == null) {
            return;
        }
        ShopCleanupMessage msg = new ShopCleanupMessage(shopId, System.currentTimeMillis());
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.SUBMIT_EXCHANGE, RabbitMQConfig.ROUTING_KEY, msg);
            log.info("shop cleanup message sent: shopId={}", shopId);
        } catch (Exception e) {
            // afterCommit 抛出会把已提交的删店 200 污染成 500，故吞掉
            log.warn("shop cleanup message publish failed, will be reconciled later: shopId={}, err={}", shopId, e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
