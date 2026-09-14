package com.tastelink.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求级 traceId 过滤器（B4-2）。
 * <p>
 * 进请求即生成短 traceId 放入 MDC（日志 pattern 内 {@code [%X{traceId}]} 可见），
 * 同时写回响应头 {@code X-Trace-Id} 便于前端/网关关联；请求结束清理 MDC 防线程池复用脏值。
 * 若上游（网关）已带 {@code X-Trace-Id}，沿用其值避免断链。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = StringUtils.hasText(request.getHeader(HEADER))
                ? request.getHeader(HEADER)
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
