package com.tastelink.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑与模板（v2 Phase C 店铺删除延时清理）。
 * <p>
 * 采用 <b>TTL + DLX</b> 免插件方案（标准 {@code rabbitmq:3-management} image 即可，本地与
 * Testcontainers 皆稳，免去 {@code rabbitmq_delayed_message_exchange} 社区插件启用风险）：
 * <pre>
 * producer → submitExchange(direct) --rk--→ delayQueue(x-message-ttl, DLX→cleanupExchange,DL-rk)
 *   delayQueue 过期 → cleanupExchange(direct) --rk--→ cleanupQueue ← consumer
 *   consumer 重试耗尽(reject, no-requeue) → cleanupQueue 的 x-dead-letter-exchange → poisonExchange --rk--→ dlq
 * </pre>
 * 固定 TTL 无「排序坑」；可靠性靠 publisher-confirm + 消费幂等 + 对账调度补投递兜底。
 * AMQP 连接懒初始化，broker 缺席时应用正常启动，仅删店后台路径降级（详见 CLAUDE.md）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    public static final String SUBMIT_EXCHANGE = "shop.cleanup.submit";
    public static final String DELAY_QUEUE = "shop.cleanup.delay.queue";
    public static final String CLEANUP_EXCHANGE = "shop.cleanup.dlx";
    public static final String CLEANUP_QUEUE = "shop.cleanup.queue";
    public static final String POISON_EXCHANGE = "shop.cleanup.poison.dlx";
    public static final String DLQ_QUEUE = "shop.cleanup.dlq";
    public static final String ROUTING_KEY = "shop.cleanup";

    private final ObjectMapper objectMapper;

    @Bean
    public DirectExchange submitExchange() {
        return new DirectExchange(SUBMIT_EXCHANGE, true, false);
    }

    @Bean
    public Queue delayQueue(@Value("${tastelink.rabbitmq.cleanup-delay-ms:5000}") long ttlMs) {
        // x-message-ttl 充当延时窗口；过期后由 x-dead-letter-exchange 转交清理队列
        return QueueBuilder.durable(DELAY_QUEUE)
                .withArgument("x-message-ttl", ttlMs)
                .withArgument("x-dead-letter-exchange", CLEANUP_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange cleanupExchange() {
        return new DirectExchange(CLEANUP_EXCHANGE, true, false);
    }

    @Bean
    public Queue cleanupQueue() {
        // 消费失败、Spring AMQP 重试耗尽后 reject(no-requeue)，由 x-dead-letter-exchange 路由进 DLQ
        return QueueBuilder.durable(CLEANUP_QUEUE)
                .withArgument("x-dead-letter-exchange", POISON_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange poisonExchange() {
        return new DirectExchange(POISON_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding delayBinding(Queue delayQueue, DirectExchange submitExchange) {
        return BindingBuilder.bind(delayQueue).to(submitExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding cleanupBinding(Queue cleanupQueue, DirectExchange cleanupExchange) {
        return BindingBuilder.bind(cleanupQueue).to(cleanupExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, DirectExchange poisonExchange) {
        return BindingBuilder.bind(dlqQueue).to(poisonExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        // publisher-confirm（yml 已配 correlated）：nack 仅记日志，靠对账调度补投递，不抛给删店主流程
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("rabbitmq publisher nack: correlationData={}, cause={}", correlationData, cause);
            }
        });
        template.setReturnsCallback(returned ->
                log.warn("rabbitmq message returned unrouted: code={}, text={}, exchange={}, rk={}",
                        returned.getReplyCode(), returned.getReplyText(),
                        returned.getExchange(), returned.getRoutingKey()));
        return template;
    }
}
