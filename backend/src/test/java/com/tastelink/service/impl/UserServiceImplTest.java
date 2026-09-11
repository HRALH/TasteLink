package com.tastelink.service.impl;

import com.tastelink.common.ResultCode;
import com.tastelink.config.RedisKeyNamespace;
import com.tastelink.dto.request.LoginRequest;
import com.tastelink.dto.response.LoginVO;
import com.tastelink.entity.User;
import com.tastelink.exception.BusinessException;
import com.tastelink.mapper.FollowMapper;
import com.tastelink.mapper.UserMapper;
import com.tastelink.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * UserServiceImpl.login 登录防爆破单测（B1-2，Mockito 不依赖 Docker）。
 * 覆盖：达阈值 42901、失败计数首击补窗口、成功清零、Redis 缺席 fail-open。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String FAIL_KEY = "tastelink:local:login:fail:tom";

    @Mock
    private UserMapper userMapper;
    @Mock
    private FollowMapper followMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private RedisKeyNamespace redisKeys;
    @Mock
    private ValueOperations<String, String> valueOps;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userMapper, followMapper, passwordEncoder, jwtUtil, redis, redisKeys);
        when(redisKeys.key(anyString())).thenAnswer(inv -> "tastelink:local:" + inv.getArgument(0));
    }

    private LoginRequest req() {
        LoginRequest r = new LoginRequest();
        r.setUsername("tom");
        r.setPassword("Abc12345");
        return r;
    }

    private User user() {
        User u = new User();
        u.setId(7L);
        u.setUsername("tom");
        u.setPassword("$2a$10$hashed");
        u.setNickname("Tom");
        u.setRole("USER");
        return u;
    }

    @Test
    void login_failCountAtThreshold_throws42901WithoutQueryingDb() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(FAIL_KEY)).thenReturn("5");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req()));

        assertEquals(ResultCode.LOGIN_RATE_LIMITED.getCode(), ex.getCode());
        assertEquals(42901, ex.getCode());
        assertEquals(200, ex.getHttpStatus());   // 与 4090x 同风格：HTTP 200 + 业务码
        verifyNoInteractions(userMapper);
    }

    @Test
    void login_wrongPassword_firstHitIncrementsAndSetsWindow() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(FAIL_KEY)).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(user());
        when(passwordEncoder.matches("Abc12345", "$2a$10$hashed")).thenReturn(false);
        when(valueOps.increment(FAIL_KEY)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req()));

        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        verify(valueOps).increment(FAIL_KEY);
        verify(redis).expire(FAIL_KEY, Duration.ofSeconds(600));   // 首击补窗口
    }

    @Test
    void login_wrongPassword_subsequentHitDoesNotRefreshWindow() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(FAIL_KEY)).thenReturn("2");              // 窗口内第 3 次失败，未达阈值
        when(userMapper.selectOne(any())).thenReturn(user());
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(valueOps.increment(FAIL_KEY)).thenReturn(3L);

        assertThrows(BusinessException.class, () -> service.login(req()));
        verify(redis, never()).expire(eq(FAIL_KEY), any(Duration.class));
    }

    @Test
    void login_success_clearsFailCount() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(FAIL_KEY)).thenReturn("3");
        when(userMapper.selectOne(any())).thenReturn(user());
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generate(7L, "tom", "USER")).thenReturn("token-1");
        when(jwtUtil.getExpireSeconds()).thenReturn(86400L);

        LoginVO vo = service.login(req());

        assertEquals("token-1", vo.token());
        verify(redis).delete(FAIL_KEY);                            // 成功清零
    }

    @Test
    void login_redisDown_failOpenStillVerifiesPassword() {
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));
        when(userMapper.selectOne(any())).thenReturn(null);        // 用户不存在

        // 限流器故障不得锁死登录：照常走账密校验，失败仍 401；计数失败也吞掉
        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req()));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
    }
}
