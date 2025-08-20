package org.yilena.myShortLink.project.utils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式短ID生成器
 * 生成7位Base62编码的短ID
 */
public class UVSymbolGenerator {
    // 起始时间戳（2020-01-01 00:00:00 UTC）
    private static final long START_TIMESTAMP = 1577836800L;
    // 实例ID位数
    private static final long INSTANCE_ID_BITS = 5L; // 32个实例
    // 序列号位数
    private static final long SEQUENCE_BITS = 9L;    // 512个序列/秒
    // 最大实例ID
    private static final long MAX_INSTANCE_ID = (1L << INSTANCE_ID_BITS) - 1;
    // 最大序列号
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    // 实例ID偏移量
    private static final long INSTANCE_ID_SHIFT = SEQUENCE_BITS;
    // 时间戳偏移量
    private static final long TIMESTAMP_SHIFT = INSTANCE_ID_BITS + SEQUENCE_BITS;
    // Base62 字符集
    private static final String BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    // 实例ID
    private final long instanceId;
    // 最后时间戳
    private long lastTimestamp = -1L;
    // 序列号
    private final AtomicLong sequence = new AtomicLong(0);

    public UVSymbolGenerator(long instanceId) {
        if (instanceId < 0 || instanceId > MAX_INSTANCE_ID) {
            throw new IllegalArgumentException("实例ID超出范围 (0 - " + MAX_INSTANCE_ID + ")");
        }
        this.instanceId = instanceId;
    }

    /**
     * 生成下一个短ID
     */
    public synchronized String nextId() {
        long currentTimestamp = getCurrentTimestamp();

        // 处理时钟回拨
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨！唯一ID已作废！");
        }

        // 处理同一秒内的序列号
        if (currentTimestamp == lastTimestamp) {
            long seq = sequence.incrementAndGet() & SEQUENCE_MASK;
            if (seq == 0) {
                currentTimestamp = waitNextTimestamp(lastTimestamp);
            }
            return encode(currentTimestamp, seq);
        }
        // 新时间戳重置序列号
        else {
            sequence.set(0);
            lastTimestamp = currentTimestamp;
            return encode(currentTimestamp, 0);
        }
    }

    /**
     * 获取当前相对时间戳（秒）
     */
    private long getCurrentTimestamp() {
        return Instant.now().getEpochSecond() - START_TIMESTAMP;
    }

    /**
     * 等待直到获得下一时间戳
     */
    private long waitNextTimestamp(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    /**
     * 编码为7位Base62字符串
     */
    private String encode(long timestamp, long sequenceVal) {
        // 组合ID：时间戳(38位) | 实例ID(5位) | 序列号(9位) = 52位
        long value = (timestamp << TIMESTAMP_SHIFT)
                | (instanceId << INSTANCE_ID_SHIFT)
                | sequenceVal;

        // 转换为7位Base62
        char[] chars = new char[7];
        for (int i = 6; i >= 0; i--) {
            chars[i] = BASE62.charAt((int)(value % 62));
            value /= 62;
        }
        return new String(chars);
    }
}