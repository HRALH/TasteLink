package com.tastelink.dto.response;

/**
 * 点赞结果：当前点赞数（点赞/取消点赞后返回，幂等操作下保持不变）。
 */
public record LikeCountVO(Integer likeCount) {
}
