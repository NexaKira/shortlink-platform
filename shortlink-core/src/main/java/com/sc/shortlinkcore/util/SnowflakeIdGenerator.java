package com.sc.shortlinkcore.util;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    // 起始时间戳(2024-01-01 00:00:00)
    private final long epoch = 1704067200000L;

    /**
     * 机器ID占用的位数：10位
     * 序列号占用的位数：12位
     */
    private final long workerIdBits = 10L;
    private final long sequenceBits = 12L;

    /**
     * 各部分的二进制最大值
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxSequence = -1L ^ (-1L << sequenceBits);

    /**
     * 各部分的左移位数（用于将各部分的值放到正确的位置上）
     */
    private final long workerIdShift = sequenceBits;
    private final long timestampShift = sequenceBits + workerIdBits;

    private long workerId;            // 机器ID
    private long sequence = 0L;       // 当前毫秒内的序列号
    private long lastTimestamp = -1L; // 上次生成ID的时间戳（毫秒）

    // 单机环境
    public SnowflakeIdGenerator(WorkerIdManager workerIdManager) {
        this(workerIdManager.getWorkerId()); // 从 Redis 动态获取
    }

    public SnowflakeIdGenerator(long workerId ) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("WorkerId 不合法：" + workerId);
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        // 时钟回拨处理
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - epoch) << timestampShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    /**
     * 循环等待直到下一毫秒
     * @param lastTimestamp 上一次生成 ID 的时间戳
     * @return 新的时间戳（大于 lastTimestamp）
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        // 只要当前时间戳 <= 上一次的时间戳，就继续自旋获取新时间
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

}
