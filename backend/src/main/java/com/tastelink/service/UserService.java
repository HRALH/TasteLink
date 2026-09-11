package com.tastelink.service;

import com.tastelink.dto.request.LoginRequest;
import com.tastelink.dto.request.RegisterRequest;
import com.tastelink.dto.request.UpdateProfileRequest;
import com.tastelink.dto.response.LoginVO;
import com.tastelink.dto.response.UserVO;
import com.tastelink.entity.User;

import java.util.Collection;
import java.util.List;

public interface UserService {

    /** 注册：返回新用户 ID */
    Long register(RegisterRequest req);

    /** 登录：校验账号密码，签发 JWT */
    LoginVO login(LoginRequest req);

    /**
     * 登出（B1-3）：把当前 token 的 jti 写入 Redis 黑名单（剩余有效期 TTL），
     * 命中黑名单的 token 再访问受保护接口返回 401。Redis 缺席时静默成功（降级为仅前端清态）。
     *
     * @param token 原始 JWT（不含 Bearer 前缀），可空
     */
    void logout(String token);

    /** 当前登录用户完整资料（含 username） */
    UserVO getCurrentUserVO();

    /** 更新本人资料（昵称/头像/简介） */
    UserVO updateUserProfile(UpdateProfileRequest req);

    /** 查看某用户主页资料（公开，不含 username） */
    UserVO getUserById(Long id);

    /** 批量转用户摘要 VO（关注/粉丝列表用），含 hasFollowed 批量计算 */
    List<UserVO> toUserVOs(Collection<User> users, Long currentUserId);
}
