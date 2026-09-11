package com.tastelink.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 店铺删除延时清理消息（v2 Phase C）。
 * <ul>
 *   <li>{@code shopId} —— 待物理清理的店铺</li>
 *   <li>{@code sentAt} —— 发送时间戳，排查用（{@link System#currentTimeMillis()}）</li>
 * </ul>
 * 消费者按 {@code shopId} 执行幂等级联清理；重复投递安全。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopCleanupMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long shopId;
    private long sentAt;
}
