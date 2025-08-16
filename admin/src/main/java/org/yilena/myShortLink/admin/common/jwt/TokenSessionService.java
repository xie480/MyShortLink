//package org.yilena.myShortLink.admin.common.jwt;
//
//import io.jsonwebtoken.Claims;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//import org.yilena.myShortLink.admin.common.convention.errorCode.codes.UserErrorCodes;
//import org.yilena.myShortLink.admin.common.convention.exception.UserException;
//import org.yilena.myShortLink.admin.common.jwt.entry.RefreshResult;
//
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//@Service
//@RequiredArgsConstructor
//public class TokenSessionService {
//    private final JwtTokenProvider tokenProvider;
//    private final StringRedisTemplate stringRedisTemplate;
//    private final RedissonClient redissonClient;
//
//    // 令牌版本
//    private static final String TOKEN_VERSION_PREFIX = "token_version:";
//    // 刷新令牌存储
//    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
//    // 短期黑名单
//    private static final String SHORT_BLACKLIST_PREFIX = "short_blackList:";
//    // 刷新次数计数器
//    private static final String REFRESH_COUNT_PREFIX = "refresh_count:";
//    // 分布式锁
//    private static final String RLOCK_PREFIX = "token_lock:";
//
//    // 创建新会话
//    public String createNewSession(String username) {
//        String newTokenVersion = UUID.randomUUID().toString();
//        // 存储刷新令牌关联信息
//        String refreshKey = REFRESH_TOKEN_PREFIX + username;
//        stringRedisTemplate.opsForValue().set(refreshKey, newTokenVersion, 1, TimeUnit.DAYS);
//        return newTokenVersion;
//    }
//
//    // 刷新令牌状态
//    public RefreshResult refreshTokenState(String refreshToken, HttpServletRequest request) throws InterruptedException {
//        // 解析刷新令牌
//        Claims claims = tokenProvider.parseRefreshToken(refreshToken);
//        String username = claims.getSubject();
//
//        // 分布式锁防止并发刷新
//        RLock lock = redissonClient.getLock(RLOCK_PREFIX + username);
//        if (!lock.tryLock(3000, TimeUnit.MILLISECONDS)) {
//            throw new UserException(UserErrorCodes.USER_ERROR);
//        }
//
//        try {
//            // 检查刷新次数，防止恶意攻击
//            // 获取唯一标识
//            String jti = claims.getId();
//            String countKey = REFRESH_COUNT_PREFIX + jti;
//            // 加了锁就不用lua脚本
//            Long refreshCount = stringRedisTemplate.opsForValue().increment(countKey);
//            stringRedisTemplate.expire(countKey, 1, TimeUnit.HOURS);
//            // 1小时内最多刷新3次
//            if (refreshCount > 3) {
//                // 直接吊销刷新令牌强制用户重新登录
//                revokeRefreshToken(username);
//                throw new SecurityException("刷新频率异常");
//            }
//
//            // 生成新令牌版本
//            String newTokenVersion = UUID.randomUUID().toString();
//            String refreshKey = REFRESH_TOKEN_PREFIX + username;
//            stringRedisTemplate.opsForValue().set(refreshKey, newTokenVersion, 1, TimeUnit.DAYS);
//
//            // 生成新双令牌
//            String newAccessToken = tokenProvider.generateToken(username, newTokenVersion);
//            String newRefreshToken = tokenProvider.generateRefreshToken(username, newTokenVersion);
//
//            // 将旧刷新令牌加入黑名单
//            long ttl = tokenProvider.getRemainingTime(refreshToken);
//            addToShortBlacklist(jti, ttl);
//
//            return new RefreshResult(newAccessToken, newRefreshToken);
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    // 验证刷新令牌有效性
//    public boolean isValidToken(String username, String tokenVersion) {
//        String refreshKey = REFRESH_TOKEN_PREFIX + username;
//        String storedVersion = stringRedisTemplate.opsForValue().get(refreshKey);
//        return tokenVersion.equals(storedVersion);
//    }
//
//    // 吊销刷新令牌
//    public void revokeRefreshToken(String username) {
//        String refreshKey = REFRESH_TOKEN_PREFIX + username;
//        stringRedisTemplate.delete(refreshKey);
//    }
//
//    // 加入短期黑名单
//    public void addToShortBlacklist(String jti, long ttlMillis) {
//        if (ttlMillis > 0) {
//            stringRedisTemplate.opsForValue().set(
//                SHORT_BLACKLIST_PREFIX + jti,
//                "1",
//                ttlMillis, TimeUnit.MILLISECONDS
//            );
//        }
//    }
//
//    // 检查是否在黑名单中
//    public boolean isJtiBlacklisted(String jti) {
//        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(SHORT_BLACKLIST_PREFIX + jti));
//    }
//}