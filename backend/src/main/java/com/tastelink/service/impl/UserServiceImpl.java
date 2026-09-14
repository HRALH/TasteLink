package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tastelink.common.Constants;
import com.tastelink.common.ResultCode;
import com.tastelink.config.RedisKeyNamespace;
import com.tastelink.dto.request.LoginRequest;
import com.tastelink.dto.request.RegisterRequest;
import com.tastelink.dto.request.UpdateProfileRequest;
import com.tastelink.dto.response.LoginVO;
import com.tastelink.dto.response.UserVO;
import com.tastelink.entity.Follow;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.FollowMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.security.JwtBlacklistService;
import com.tastelink.security.JwtUtil;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 密码：长度≥8 且同时含字母与数字 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    /** 登录防爆破（B1-2）：窗口内失败次数上限与窗口时长（秒） */
    private static final long LOGIN_FAIL_LIMIT = 5;
    private static final long LOGIN_FAIL_WINDOW_SECONDS = 600;

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final RedisKeyNamespace redisKeys;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    public Long register(RegisterRequest req) {
        if (!PASSWORD_PATTERN.matcher(req.getPassword()).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码需至少 8 位且包含字母与数字");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getUsername());
        user.setFollowingCount(0);
        user.setFollowerCount(0);
        user.setReviewCount(0);
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.STATUS_NORMAL);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        return user.getId();
    }

    @Override
    public LoginVO login(LoginRequest req) {
        // 登录防爆破（B1-2）：Redis 计数 login:fail:{username}，600s 窗口内失败 ≥5 次拒绝。
        // 独立于 RANK_CACHE_ENABLED（该开关只管热榜）；Redis 缺席/异常一律放行 + warn，
        // 绝不因限流器故障锁死登录。
        checkLoginRateLimit(req.getUsername());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())
                .eq(User::getStatus, Constants.STATUS_NORMAL));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            recordLoginFailure(req.getUsername());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        clearLoginFailures(req.getUsername());
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, jwtUtil.getExpireSeconds(), user.getId(),
                user.getUsername(), user.getNickname(), user.getAvatarUrl());
    }

    /** 失败计数达阈值则抛 42901；Redis 异常放行（fail-open）。 */
    private void checkLoginRateLimit(String username) {
        try {
            String v = redis.opsForValue().get(loginFailKey(username));
            if (v != null && Long.parseLong(v) >= LOGIN_FAIL_LIMIT) {
                throw new BusinessException(ResultCode.LOGIN_RATE_LIMITED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("login rate-limit check failed, allow attempt: username={}, err={}", username, e.getMessage());
        }
    }

    /** 记录一次失败：INCR，首击补 EXPIRE 窗口；Redis 异常吞掉。 */
    private void recordLoginFailure(String username) {
        try {
            String key = loginFailKey(username);
            Long n = redis.opsForValue().increment(key);
            if (n != null && n == 1L) {
                redis.expire(key, Duration.ofSeconds(LOGIN_FAIL_WINDOW_SECONDS));
            }
        } catch (Exception e) {
            log.warn("login failure record skipped: username={}, err={}", username, e.getMessage());
        }
    }

    /** 登录成功清零失败计数；Redis 异常吞掉。 */
    private void clearLoginFailures(String username) {
        try {
            redis.delete(loginFailKey(username));
        } catch (Exception e) {
            log.warn("login failure clear skipped: username={}, err={}", username, e.getMessage());
        }
    }

    private String loginFailKey(String username) {
        return redisKeys.key("login:fail:" + username);
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            Date exp = claims.getExpiration();
            long remaining = exp == null ? 0 : (exp.getTime() - System.currentTimeMillis()) / 1000;
            jwtBlacklistService.revoke(claims.getId(), remaining);
        } catch (Exception e) {
            // token 已过期/非法：无可吊销，静默成功（前端照常清本地态）
            log.debug("logout with unparseable token, skip blacklist: {}", e.getMessage());
        }
    }

    @Override
    public UserVO getCurrentUserVO() {
        Long userId = SecurityContextHelper.requireCurrentUserId();
        return toMeVO(mustGet(userId));
    }

    @Override
    public UserVO updateUserProfile(UpdateProfileRequest req) {
        Long userId = SecurityContextHelper.requireCurrentUserId();
        mustGet(userId);
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.eq(User::getId, userId);
        if (StringUtils.hasText(req.getNickname())) {
            uw.set(User::getNickname, req.getNickname());
        }
        if (StringUtils.hasText(req.getAvatarUrl())) {
            uw.set(User::getAvatarUrl, req.getAvatarUrl());
        }
        if (req.getBio() != null) {
            uw.set(User::getBio, req.getBio());
        }
        userMapper.update(null, uw);
        return toMeVO(userMapper.selectById(userId));
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, id)
                .eq(User::getStatus, Constants.STATUS_NORMAL));
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long currentId = SecurityContextHelper.getCurrentUserId();
        boolean hasFollowed = currentId != null && !currentId.equals(id) && hasFollowed(currentId, id);
        return toProfileVO(user, hasFollowed);
    }

    @Override
    public List<UserVO> toUserVOs(Collection<User> users, Long currentUserId) {
        List<User> list = users == null ? List.of() : List.copyOf(users);
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> ids = list.stream().map(User::getId).toList();
        Set<Long> followedIds = Collections.emptySet();
        if (currentUserId != null) {
            List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getFollowerId, currentUserId)
                    .in(Follow::getFolloweeId, ids));
            followedIds = follows.stream().map(Follow::getFolloweeId).collect(Collectors.toSet());
        }
        final Set<Long> finalFollowed = followedIds;
        return list.stream()
                .map(u -> UserVO.builder()
                        .id(u.getId())
                        .nickname(u.getNickname())
                        .avatarUrl(u.getAvatarUrl())
                        .bio(u.getBio())
                        .followingCount(u.getFollowingCount())
                        .followerCount(u.getFollowerCount())
                        .reviewCount(u.getReviewCount())
                        .hasFollowed(finalFollowed.contains(u.getId()))
                        .build())
                .toList();
    }

    private User mustGet(Long id) {
        User u = userMapper.selectById(id);
        if (u == null || (u.getStatus() != null && u.getStatus() == Constants.STATUS_HIDDEN)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return u;
    }

    private boolean hasFollowed(Long followerId, Long followeeId) {
        Long c = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFolloweeId, followeeId));
        return c != null && c > 0;
    }

    private UserVO toMeVO(User u) {
        return UserVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .bio(u.getBio())
                .followingCount(u.getFollowingCount())
                .followerCount(u.getFollowerCount())
                .reviewCount(u.getReviewCount())
                .hasFollowed(false)
                .build();
    }

    private UserVO toProfileVO(User u, boolean hasFollowed) {
        return UserVO.builder()
                .id(u.getId())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .bio(u.getBio())
                .followingCount(u.getFollowingCount())
                .followerCount(u.getFollowerCount())
                .reviewCount(u.getReviewCount())
                .hasFollowed(hasFollowed)
                .build();
    }
}
