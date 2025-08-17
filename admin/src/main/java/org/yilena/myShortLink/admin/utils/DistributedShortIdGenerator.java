package org.yilena.myShortLink.admin.utils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式短ID生成器
 * 生成6位Base62编码的短ID
 */
public class DistributedShortIdGenerator {
    // 实例ID
    private final long instanceId;
    // 最后时间戳
    private long lastTimestamp = -1L;
    // 序列号
    private final AtomicLong sequence = new AtomicLong(0);
    // Base62 字符集
    private static final String BASE62 =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public DistributedShortIdGenerator(long instanceId) {
        if (instanceId < 0 || instanceId > 31) {
            throw new IllegalArgumentException("实例序列码超出范围！");
        }
        this.instanceId = instanceId;
    }

    /**
     * 生成下一个短ID
     */
    public synchronized String nextId() {
        // 获取当前时间戳
        long currentTimestamp = timeGen();
        // 如果时钟回拨
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨！唯一ID已作废！");
        }

        // 如果时间戳相同，则使用序列号
        if (currentTimestamp == lastTimestamp) {
            // 限制序列号范围，1毫秒最多生成512个ID
            long seq = sequence.incrementAndGet() & 0x1FF;
            // 如果序列号已满，则等待下一毫秒
            if (seq == 0) {
                // 等待下一毫秒
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
            return encode(currentTimestamp, seq);
        } else {
            // 时间戳不同，则重置序列号
            sequence.set(0);
            lastTimestamp = currentTimestamp;
            return encode(currentTimestamp, 0);
        }
    }

    /**
     * 等待直到获得一个更大的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        // 获取当前时间戳
        long timestamp = timeGen();
        // 循环等待时间错增长
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     */
    private long timeGen() {
        // 使用秒级时间戳
        return Instant.now().getEpochSecond();
    }

    /**
     * 编码时间戳和序列号为Base62字符串
     */
    private String encode(long timestamp, long sequenceVal) {
        // 合并时间戳 + 实例ID + 序列号 (共35位)
        long value = ((timestamp << 14) // 26位时间戳
                   | (instanceId << 9)  // 5位实例ID
                   | sequenceVal);      // 9位序列号

        // 转换为6位Base62
        char[] buffer = new char[6];
        for (int i = 5; i >= 0; i--) {
            buffer[i] = BASE62.charAt((int)(value % 62));
            value = value / 62;
        }
        return new String(buffer);
    }
}
