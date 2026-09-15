package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.R;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.NotificationVO;
import com.tastelink.dto.response.UnreadCountVO;
import com.tastelink.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知（产品优化 F1）：顶栏红点的未读数 + 通知分页 + 标记已读。
 * 全部需登录（SecurityConfig 默认 anyRequest().authenticated()，未在 permitAll 白名单内）。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public R<PageResult<NotificationVO>> list(PageQuery pq) {
        return R.ok(notificationService.listMine(pq));
    }

    @GetMapping("/unread-count")
    public R<UnreadCountVO> unreadCount() {
        return R.ok(new UnreadCountVO(notificationService.unreadCount()));
    }

    @PostMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok(null);
    }

    @PostMapping("/read-all")
    public R<Void> markAllRead() {
        notificationService.markAllRead();
        return R.ok(null);
    }
}
