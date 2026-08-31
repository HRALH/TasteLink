package com.tastelink.dto.response;

/**
 * 登录返回：token、有效期及用户基础信息。
 */
public record LoginVO(String token, long expiresInSec, Long userId, String username,
                      String nickname, String avatarUrl) {
}
