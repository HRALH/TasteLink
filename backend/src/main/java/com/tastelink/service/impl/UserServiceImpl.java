package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tastelink.common.Constants;
import com.tastelink.common.ResultCode;
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
import com.tastelink.security.JwtUtil;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 密码：长度≥8 且同时含字母与数字 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public Long register(RegisterRequest req) {
        if (!PASSWORD_PATTERN.matcher(req.getPassword()).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码需至少 8 位且包含字母与数字");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getUsername());
        user.setFollowingCount(0);
        user.setFollowerCount(0);
        user.setReviewCount(0);
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
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())
                .eq(User::getStatus, Constants.STATUS_NORMAL));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        return new LoginVO(token, jwtUtil.getExpireSeconds(), user.getId(),
                user.getUsername(), user.getNickname(), user.getAvatarUrl());
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
    public User getUserEntity(Long id) {
        return userMapper.selectById(id);
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
