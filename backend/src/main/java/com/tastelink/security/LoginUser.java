package com.tastelink.security;

import com.tastelink.common.Constants;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * SecurityContext 中的登录主体，承载 userId / username / role（密码不入内存）。
 * <p>
 * role 取值 {@link Constants#ROLE_USER} / {@link Constants#ROLE_ADMIN}；
 * {@link #getAuthorities()} 输出 {@code ROLE_<role>} 形式，使 SecurityConfig 的
 * {@code hasRole('ADMIN')} 规则生效（hasRole 自动补 ROLE_ 前缀）— v2 Phase B。
 */
@Getter
public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String role;

    public LoginUser(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role != null ? role : Constants.ROLE_USER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security 的 hasRole(...) 会自动加 ROLE_ 前缀匹配，故此处补 ROLE_。
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
