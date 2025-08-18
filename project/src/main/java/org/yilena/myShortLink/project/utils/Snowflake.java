package org.yilena.myShortLink.project.utils;

import org.springframework.stereotype.Component;
import org.yilena.myShortLink.project.common.convention.exception.SystemException;

import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class Snowflake {
    // 起始时间戳
    private static final long START_TIMESTAMP = 1672531200000L; 
    
    // 机器ID位数
    private static final long WORKER_ID_BITS = 8L;
    // 数据中心ID位数
    private static final long DATACENTER_ID_BITS = 8L;
    // 最大机器ID
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 最大数据中心ID
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    
    // 序列号位数
    private static final long SEQUENCE_BITS = 12L;
    // 机器ID左移位数
    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    // 数据中心ID左移位数
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    // 时间戳左移位数
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    // 最大序列号
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    // 机器ID
    private final long workerId;
    // 数据中心ID
    private final long datacenterId;
    // 序列号
    private long sequence = 0L;
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;

    public Snowflake() {
        // 从系统环境获取机器信息
        this.workerId = initWorkerId();
        this.datacenterId = initDatacenterId();
    }

    // 初始化机器ID
    private long initWorkerId() {
        try {
            // 获取本机IP地址
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            // 获取IP地址最后一段
            String[] segments = hostAddress.split("\\.");
            // 返回机器ID
            return Long.parseLong(segments[3]) & MAX_WORKER_ID;
        } catch (Exception e) {
            // 获取失败，返回随机数
            return ThreadLocalRandom.current().nextLong(MAX_WORKER_ID);
        }
    }

    // 初始化数据中心ID
    private long initDatacenterId() {
        try {
            // 获取本机主机名
            String hostName = InetAddress.getLocalHost().getHostName();
            // 哈希后置取模
            return Math.abs(hostName.hashCode()) % (MAX_DATACENTER_ID + 1);
        } catch (Exception e) {
            // 获取失败，返回随机数
            return ThreadLocalRandom.current().nextLong(MAX_DATACENTER_ID);
        }
    }

    // 生成ID
    public synchronized long nextId() {
        // 获取当前时间戳
        long timestamp = System.currentTimeMillis();

        // 如果时钟回拨
        if (timestamp < lastTimestamp) {
            throw new SystemException("时钟回拨！唯一ID失效！");
        }

        // 时间戳相同，则使用序列号
        if (lastTimestamp == timestamp) {
            // 限制序列号范围，1毫秒最多生成512个ID
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 如果序列号已满，则等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    // 等待下一毫秒
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}