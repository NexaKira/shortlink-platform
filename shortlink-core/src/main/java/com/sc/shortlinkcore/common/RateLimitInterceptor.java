package com.sc.shortlinkcore.common;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.sc.shortlinkcommon.RateLimit;
import com.sc.shortlinkcommon.BusinessException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final StringRedisTemplate redisTemplate; // 操作 Redis 的工具
    private final DefaultRedisScript<Long> redisScript;    // 封装 Lua 的脚本
    private final ConcurrentHashMap<String, RateLimiter> fallbackLimiters; // Guava 降级用

    // Redis 状态标记
    private volatile boolean redisAvailable = true;
    private volatile long lastRedisFailureTime = 0;
    private static final long REDIS_RETRY_MS = 30_000; // 30 秒后重试 Redis

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.fallbackLimiters = new ConcurrentHashMap<>();

        // 从 resources 目录加载 Lua 脚本
        this.redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new org.springframework.core.io.ClassPathResource("rate_limit.lua"));
        redisScript.setResultType(Long.class);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 不是方法调用（比如静态资源请求），直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        // 拿方法上的 @RateLimit 注解，没注解就直接放行
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String methodName = handlerMethod.getMethod().getName();
        String message = rateLimit.message();

        // 主策略：Redis Lua 限流
        if (redisAvailable || shouldRetryRedis()) {
            try {
                if (tryRedisAcquire(request, rateLimit, methodName)) {
                    return true;
                } else {
                    throw new BusinessException(429, message);
                }
            } catch (Exception e) {
                if (e instanceof BusinessException) {
                    throw (BusinessException) e;
                }
                // Redis 自身出了异常（连不上等），降级
                log.warn("Redis 限流失败，降级到 Guava：{}", e.getMessage());
                redisAvailable = false;
                lastRedisFailureTime = System.currentTimeMillis();
            }
        }

        // 降级策略：Guava 本地限流
        if (!tryGuavaAcquire(rateLimit, methodName)) {
            throw new BusinessException(429, message);
        }
        return true;
    }

    private boolean shouldRetryRedis() {
        return System.currentTimeMillis() - lastRedisFailureTime > REDIS_RETRY_MS;
    }

    private boolean tryGuavaAcquire(RateLimit rateLimit, String methodName) {
        RateLimiter limiter = fallbackLimiters.computeIfAbsent(
                methodName,
                k -> RateLimiter.create(rateLimit.permitsPerSecond())
        );
        return limiter.tryAcquire();
    }

    private boolean tryRedisAcquire(HttpServletRequest request, RateLimit rateLimit, String methodName) {
        // 组装 Redis key
        String key = buildRedisKey(request, rateLimit, methodName);

        // 生成唯一 member ID（时间戳 + 随机数，防止同一毫秒内成员重名）
        String memberId = System.currentTimeMillis() + "-"
                + ThreadLocalRandom.current().nextInt(1_000_000);

        // 准备参数传给 Lua 脚本
        String[] argv = {
                String.valueOf(rateLimit.windowSeconds()),
                String.valueOf((long) rateLimit.permitsPerSecond()),
                String.valueOf(System.currentTimeMillis()),
                memberId
        };

        // 执行 Lua 脚本
        Long result = redisTemplate.execute(redisScript, List.of(key), argv);

        if (result != null) {
            redisAvailable = true; // Redis 正常，标记可用
        }
        return result != null && result == 1L;

    }

    private String buildRedisKey(HttpServletRequest request, RateLimit rateLimit, String methodName) {
        String prefix = "rate_limit:" + methodName;
        if (rateLimit.limitType() == RateLimit.LimitType.GLOBAL) {
            return prefix + ":global";
        } else {
            // 优先取 X-Forwarded-For 的第一个 IP（真实客户端）
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            } else {
                ip = ip.split(",")[0].trim(); // 取第一个，去掉空格
            }
            return prefix + ":ip:" + ip;
        }
    }
}
