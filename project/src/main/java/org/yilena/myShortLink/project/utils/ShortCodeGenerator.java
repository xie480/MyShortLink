package org.yilena.myShortLink.project.utils;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.yilena.myShortLink.project.common.convention.exception.SystemException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class ShortCodeGenerator {
    private final StringRedisTemplate stringRedisTemplate;
    private final Snowflake snowflake;
    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;

    // 短码生成算法参数
    private static final long MODULUS = 56_800_235_584L;
    // 最大重试次数
    private static final int MAX_RETRIES = 5;

    /**
     * 生成全局唯一的6位短码
     */
    public String generateUniqueShortCode(String originalUrl) {
        int retryCount = 0;
        while (retryCount <= MAX_RETRIES) {
            String candidate = generateCandidate(originalUrl);
            // 判断布隆过滤器中是否存在
            if (Boolean.FALSE.equals(shortUriCreateCachePenetrationBloomFilter.contains(candidate))) {
                // 因为不存在的判定是百分百存在的，所以我们只需要判断不存在就行了，对于不存在却判断为存在的情况我们直接无视即可
                // 放入
                shortUriCreateCachePenetrationBloomFilter.add(candidate);
                return candidate;
            }
            retryCount++;
        }
        throw new SystemException("短链接唯一后缀生成出错！");
    }

    /**
     * 生成候选短码
     */
    private String generateCandidate(String originalUrl) {
        try {
            // 计算URL的SHA-256哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(originalUrl.getBytes(StandardCharsets.UTF_8));
            
            // 提取前8字节作为基础值
            long urlHashPart = ByteBuffer.wrap(hash, 0, 8).getLong();
            
            // 获取雪花算法ID
            long snowflakeId = snowflake.nextId();
            
            // 混合算法（XOR + 位操作）
            long mixed = (urlHashPart ^ snowflakeId) & Long.MAX_VALUE;
            
            // 取模映射到6位空间
            long mappedValue = mixed % MODULUS;
            
            // Base62编码
            return Base62Encoder.encode(mappedValue, 6);
        } catch (Exception e) {
            // 其实这种报错都应该使用warn告警错误码的，但是因为我嫌麻烦就敷衍敷衍得了
            throw new SystemException("短链接唯一后缀生成出错！");
        }
    }
}