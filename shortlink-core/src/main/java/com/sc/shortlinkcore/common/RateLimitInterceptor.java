package com.sc.shortlinkcore.common;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.sc.shortlinkcommon.RateLimit;
import com.sc.shortlinkcommon.BusinessException;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            String key = handlerMethod.getMethod().getName();
            RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimit != null) {
                // 日志
                System.out.println("RateLimit hit on method:" + key);
                RateLimiter limiter = limiters.computeIfAbsent(key, k -> RateLimiter.create(rateLimit.permitsPerSecond()));
                // 尝试获取令牌
                if (!limiter.tryAcquire()) {
                    throw new BusinessException(429, rateLimit.message());
                }
            }
        }
        return true;
    }
}
