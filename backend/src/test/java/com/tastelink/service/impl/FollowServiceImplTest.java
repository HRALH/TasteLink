package com.tastelink.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.entity.Follow;
import com.tastelink.entity.User;
import com.tastelink.mapper.FollowMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FollowServiceImpl 单测（B2-1）：关注/粉丝列表按关注时间序（分页 id 序）重排，
 * 不被 selectBatchIds 的主键序打散。
 */
@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private FollowMapper followMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserService userService;

    private FollowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FollowServiceImpl(followMapper, userMapper, userService);
    }

    private Follow follow(long followerId, long followeeId) {
        Follow f = new Follow();
        f.setFollowerId(followerId);
        f.setFolloweeId(followeeId);
        return f;
    }

    private User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    @SuppressWarnings("unchecked")
    private void stubFollowPage(List<Follow> records) {
        when(followMapper.selectPage(any(), any())).thenAnswer(inv -> {
            Page<Follow> p = inv.getArgument(0);
            p.setRecords(records);
            p.setTotal(records.size());
            return p;
        });
    }

    @Test
    void listFollowings_preservesFollowTimeOrder() {
        // 关注时间倒序 id 序：[5, 3, 9]；selectBatchIds 模拟返回主键序 [3, 5, 9] 打散
        stubFollowPage(List.of(follow(1L, 5L), follow(1L, 3L), follow(1L, 9L)));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(3L), user(5L), user(9L)));

        service.listFollowings(1L, new PageQuery());

        ArgumentCaptor<Collection<User>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userService).toUserVOs(captor.capture(), nullable(Long.class));
        List<Long> order = captor.getValue().stream().map(User::getId).toList();
        assertEquals(List.of(5L, 3L, 9L), order, "必须保持关注时间序，不被 IN 查询主键序打散");
    }

    @Test
    void listFollowers_preservesOrderAndSkipsMissing() {
        // 粉丝 id 序 [8, 2]；id=2 的用户已被物理删除（selectBatchIds 取不到）→ 跳过
        stubFollowPage(List.of(follow(8L, 1L), follow(2L, 1L)));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(8L)));

        service.listFollowers(1L, new PageQuery());

        ArgumentCaptor<Collection<User>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userService).toUserVOs(captor.capture(), nullable(Long.class));
        List<Long> order = captor.getValue().stream().map(User::getId).toList();
        assertEquals(List.of(8L), order);
    }

    @Test
    void listFollowings_emptyPage_skipsBatchLoad() {
        stubFollowPage(List.of());

        service.listFollowings(1L, new PageQuery());

        org.mockito.Mockito.verifyNoInteractions(userMapper);
    }
}
