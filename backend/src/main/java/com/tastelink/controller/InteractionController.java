package com.tastelink.controller;

import com.tastelink.common.Constants;
import com.tastelink.common.R;
import com.tastelink.dto.request.CreateCommentRequest;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.CommentVO;
import com.tastelink.dto.response.LikeCountVO;
import com.tastelink.common.PageResult;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 互动：点赞/取消点赞（幂等）、评论列表/发表评论。
 * 与 ReviewController 共享 /reviews 前缀，按子路径区分（likes / comments）。
 */
@RestController
@RequestMapping(Constants.API_V1 + "/reviews")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/{reviewId}/likes")
    public R<LikeCountVO> like(@PathVariable Long reviewId) {
        Long userId = SecurityContextHelper.requireCurrentUserId();
        return R.ok(interactionService.like(reviewId, userId));
    }

    @DeleteMapping("/{reviewId}/likes")
    public R<LikeCountVO> unlike(@PathVariable Long reviewId) {
        Long userId = SecurityContextHelper.requireCurrentUserId();
        return R.ok(interactionService.unlike(reviewId, userId));
    }

    @GetMapping("/{reviewId}/comments")
    public R<PageResult<CommentVO>> listComments(@PathVariable Long reviewId, PageQuery pq) {
        return R.ok(interactionService.listComments(reviewId, pq));
    }

    @PostMapping("/{reviewId}/comments")
    public R<CommentVO> createComment(@PathVariable Long reviewId,
                                       @Valid @RequestBody CreateCommentRequest req) {
        Long userId = SecurityContextHelper.requireCurrentUserId();
        return R.ok(interactionService.createComment(reviewId, userId, req.getContent()));
    }
}
