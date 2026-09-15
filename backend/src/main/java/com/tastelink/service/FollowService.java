package com.tastelink.service;

import com.tastelink.common.PageResult;
import com.tastelink.dto.request.PageQuery;
import com.tastelink.dto.response.FollowCountVO;
import com.tastelink.dto.response.UserVO;

import java.util.List;

public interface FollowService {

    /** 关注用户（幂等；不可关注自己）返回操作者关注数 */
    FollowCountVO follow(Long followeeId, Long userId);

    /** 取消关注（幂等）返回操作者关注数 */
    FollowCountVO unfollow(Long followeeId, Long userId);

    /** 某用户的关注列表 */
    PageResult<UserVO> listFollowings(Long userId, PageQuery pq);

    /** 某用户的粉丝列表 */
    PageResult<UserVO> listFollowers(Long userId, PageQuery pq);

    /** 我关注的人的 id 列表（产品优化 F3，feed 查询用；不分页，实际关注数有界） */
    List<Long> followeeIds(Long userId);
}
