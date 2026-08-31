package com.tastelink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.common.Constants;
import com.tastelink.common.PageResult;
import com.tastelink.common.ResultCode;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.FollowCountVO;
import com.tastelink.dto.response.UserVO;
import com.tastelink.entity.Follow;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.FollowMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.security.SecurityContextHelper;
import com.tastelink.service.FollowService;
import com.tastelink.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FollowCountVO follow(Long followeeId, Long userId) {
        if (followeeId.equals(userId)) {
            throw new BusinessException(ResultCode.CANNOT_FOLLOW_SELF);
        }
        User followee = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, followeeId)
                .eq(User::getStatus, Constants.STATUS_NORMAL));
        if (followee == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        try {
            Follow f = new Follow();
            f.setFollowerId(userId);
            f.setFolloweeId(followeeId);
            followMapper.insert(f);
            // 操作者关注数 +1，被关注者粉丝数 +1，同事务维护
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .setSql("following_count = following_count + 1"));
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, followeeId)
                    .setSql("follower_count = follower_count + 1"));
        } catch (DuplicateKeyException dup) {
            // 已关注：幂等返回当前关注数
        }
        Integer followingCount = userMapper.selectById(userId).getFollowingCount();
        return new FollowCountVO(followingCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FollowCountVO unfollow(Long followeeId, Long userId) {
        int affected = followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .eq(Follow::getFolloweeId, followeeId));
        if (affected > 0) {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .setSql("following_count = GREATEST(0, following_count - 1)"));
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, followeeId)
                    .setSql("follower_count = GREATEST(0, follower_count - 1)"));
        }
        Integer followingCount = userMapper.selectById(userId).getFollowingCount();
        return new FollowCountVO(followingCount);
    }

    @Override
    public PageResult<UserVO> listFollowings(Long userId, PageQuery pq) {
        Page<Follow> page = new Page<>(pq.getPageOrDefault(), pq.getSizeOrDefault());
        followMapper.selectPage(page, new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .orderByDesc(Follow::getCreateTime));
        List<Long> followeeIds = page.getRecords().stream().map(Follow::getFolloweeId).toList();
        return PageResult.from(page, toUserVOs(followeeIds));
    }

    @Override
    public PageResult<UserVO> listFollowers(Long userId, PageQuery pq) {
        Page<Follow> page = new Page<>(pq.getPageOrDefault(), pq.getSizeOrDefault());
        followMapper.selectPage(page, new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId)
                .orderByDesc(Follow::getCreateTime));
        List<Long> followerIds = page.getRecords().stream().map(Follow::getFollowerId).toList();
        return PageResult.from(page, toUserVOs(followerIds));
    }

    private List<UserVO> toUserVOs(List<Long> userIds) {
        List<User> users = userIds.isEmpty() ? List.of() : userMapper.selectBatchIds(userIds);
        return userService.toUserVOs(users, SecurityContextHelper.getCurrentUserId());
    }
}
