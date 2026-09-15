package com.tastelink.service;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.NotificationVO;

/**
 * 站内通知（产品优化 F1）。
 * <p>
 * 写语义：{@code create} 与核心互动（点赞/评论/关注）同事务原子提交，内部吞异常绝不抛出——
 * 通知是"非关键"正反馈，其失败不得阻塞核心写；与 {@code HotRankService.onLike} 的吞异常约定一致。
 */
public interface NotificationService {

    /**
     * 写一条通知。actor == user（自己互动自己）直接跳过；任何异常被吞掉记 warn，绝不抛出。
     *
     * @param userId     接收者（被互动方）
     * @param actorId    触发者
     * @param type       通知类型（见 Constants.NOTIFY_*）
     * @param targetType 目标类型（见 Constants.TARGET_*）
     * @param targetId   目标 ID
     * @param preview    摘要（评论内容片段等，可为空串）
     */
    void create(Long userId, Long actorId, String type, String targetType, Long targetId, String preview);

    /** 分页查本人的通知（按时间倒序）。 */
    PageResult<NotificationVO> listMine(PageQuery pq);

    /** 未读数。 */
    long unreadCount();

    /** 标记单条已读（幂等：不存在/已读都算成功）。 */
    void markRead(Long id);

    /** 全部已读。 */
    void markAllRead();
}
