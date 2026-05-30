package com.sc.shortlinkcore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class WorkerIdManager {

    private static final Logger log = LoggerFactory.getLogger(WorkerIdManager.class);
    private static final long MAX_WORKER_ID = 1023L;
    private static final String COUNTER_KEY = "worker:id:counter";
    private static final String HEARTBEAT_PREFIX = "worker:heartbeat:";

    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService scheduler;

    private volatile long workerId = -1L;       // -1 表示未初始化
    private volatile boolean released = false; // 是否已归还

    public WorkerIdManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "worker-heartbeat");
            t.setDaemon(true); // 守护线程：主程序退出时自动终止
            return t;
        });
    }

    public long getWorkerId() {
        if (workerId > 0) {
            return workerId; // 已经分配过了，直接返回
        }

        synchronized (this) { // 防止并发调用
            if (workerId > 0) {
                return workerId; // 双重检查
            }

            // 从 Redis 拿编号
            Long id = redisTemplate.opsForValue().increment(COUNTER_KEY);
            if (id == null || id > MAX_WORKER_ID + 1) {
                throw new IllegalStateException("WorkerId 超出范围或 Redis 不可用：" + id);
            }
            workerId = id - 1; // 变成 0 ~ 1023，用完 1024 个位置

            // 启动心跳，每 5 秒续约一次
            String heartbeatKey = HEARTBEAT_PREFIX + workerId;
            scheduler.scheduleAtFixedRate(() -> {
                if (!released) {
                    redisTemplate.opsForValue().set(heartbeatKey, "alive", 10, TimeUnit.SECONDS);
                }
            }, 5, 5, TimeUnit.SECONDS);

            log.info("成功获取 WorkerId: {}", workerId);
            return workerId;
        }
    }

    public void release() {
        if (workerId < 0 || released) {
            return;
        }
        synchronized (this) {
            if (workerId < 0 || released) {
                return;
            }
            released = true;

            // 删除心跳 key
            redisTemplate.delete(HEARTBEAT_PREFIX + workerId);

            // 归还编号(这里归还会导致 ID 碰撞)
            //redisTemplate.opsForValue().decrement(COUNTER_KEY);

            // 关闭心跳线程
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("WorkerId: {} 已归还", workerId);
            workerId = -1L;
        }
    }

}
