package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.NotificationVO;
import com.tastelink.entity.Notification;
import com.tastelink.entity.User;
import com.tastelink.mapper.NotificationMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.NotificationService;
import com.tastelink.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @Override
    public void create(Long userId, Long actorId, String type, String targetType, Long targetId, String preview) {
        // 自己互动自己不发通知（自己赞/评自己的点评；关注已被 CANNOT_FOLLOW_SELF 拦截，此处为兜底）
        if (userId == null || actorId == null || userId.equals(actorId)) {
            return;
        }
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setActorId(actorId);
            n.setType(type);
            n.setTargetType(targetType);
            n.setTargetId(targetId);
            // preview 列长度 255，截断保护
            String p = preview == null ? "" : preview;
            if (p.length() > 255) {
                p = p.substring(0, 255);
            }
            n.setPreview(p);
            n.setIsRead(0);
            notificationMapper.insert(n);
        } catch (Exception e) {
            // 通知是非关键正反馈，失败绝不阻塞核心写（点赞/评论/关注本体已在调用方事务中提交）
            log.warn("create notification failed (swallowed): userId={}, actorId={}, type={}, err={}",
                    userId, actorId, type, e.getMessage());
        }
    }

    @Override
    public PageResult<NotificationVO> listMine(PageQuery pq) {
        Long me = SecurityContextHelper.requireCurrentUserId();
        Page<Notification> page = new Page<>(pq.getPageOrDefault(), pq.getSizeOrDefault());
        notificationMapper.selectPage(page, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, me)
                .orderByDesc(Notification::getCreateTime));
        List<Notification> rows = page.getRecords();
        // 批量回查 actor 昵称/头像（关系式，避免快照过期；与 assemble 同手法）
        List<Long> actorIds = rows.stream().map(Notification::getActorId).distinct().toList();
        Map<Long, User> actorMap = actorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(actorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        List<NotificationVO> vos = rows.stream().map(n -> toVO(n, actorMap.get(n.getActorId()))).toList();
        return PageResult.from(page, vos);
    }

    @Override
    public long unreadCount() {
        Long me = SecurityContextHelper.requireCurrentUserId();
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, me)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public void markRead(Long id) {
        Long me = SecurityContextHelper.requireCurrentUserId();
        // 幂等：不存在/已读都算成功（affected==0 不报错）
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, me)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    @Override
    public void markAllRead() {
        Long me = SecurityContextHelper.requireCurrentUserId();
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, me)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    private NotificationVO toVO(Notification n, User actor) {
        return NotificationVO.builder()
                .id(n.getId())
                .type(n.getType())
                .actorId(n.getActorId())
                .actorNickname(actor == null ? null : actor.getNickname())
                .actorAvatarUrl(actor == null ? null : actor.getAvatarUrl())
                .targetType(n.getTargetType())
                .targetId(n.getTargetId())
                .preview(n.getPreview())
                .isRead(n.getIsRead() != null && n.getIsRead() == 1)
                .createTime(DateUtil.format(n.getCreateTime()))
                .build();
    }
}
