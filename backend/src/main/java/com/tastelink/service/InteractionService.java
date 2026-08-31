package com.tastelink.service;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.CommentVO;
import com.tastelink.dto.response.LikeCountVO;

public interface InteractionService {

    /** 点赞点评（幂等：已点赞返回当前计数不变） */
    LikeCountVO like(Long reviewId, Long userId);

    /** 取消点赞（幂等：未点过赞也返回成功、计数不变） */
    LikeCountVO unlike(Long reviewId, Long userId);

    /** 点评下评论列表（单层） */
    PageResult<CommentVO> listComments(Long reviewId, PageQuery pq);

    /** 发表评论（同事务维护点评 reply_count） */
    CommentVO createComment(Long reviewId, Long userId, String content);
}
