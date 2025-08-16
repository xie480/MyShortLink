//package org.yilena.myShortLink.admin.common.jwt;
//
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.security.Keys;
//import lombok.Getter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.SecretKey;
//import java.util.Base64;
//import java.util.Date;
//import java.util.UUID;
//
//@Component
//public class JwtTokenProvider {
//
//    // 密钥
//    @Value("${jwt.secret}")
//    private String secret;
//
//    // 访问令牌过期时间（毫秒）
//    @Value("${jwt.access-expiration}")
//    private long accessExpiration;
//
//    // 刷新令牌过期时间（毫秒）
//    @Getter
//    @Value("${jwt.refresh-expiration}")
//    private long refreshExpiration;
//
//    // 刷新阈值（毫秒）
//    @Value("${jwt.refresh-threshold}")
//    private long refreshThreshold;
//
//    // 安全密钥生成（线程安全）
//    private SecretKey getSigningKey() {
//        byte[] keyBytes = Base64.getDecoder().decode(secret);
//        return Keys.hmacShaKeyFor(keyBytes);
//    }
//
//    // 生成访问令牌
//    public String generateToken(String username, String tokenVersion) {
//        return buildToken(username, tokenVersion, accessExpiration, "access");
//    }
//
//    // 生成刷新令牌
//    public String generateRefreshToken(String username, String tokenVersion) {
//        return buildToken(username, tokenVersion, refreshExpiration, "refresh");
//    }
//
//    // 构建JWT令牌
//    private String buildToken(String username, String tokenVersion, long expirationMillis, String tokenType) {
//        // 唯一标识符
//        String jti = UUID.randomUUID().toString();
//
//        return Jwts.builder()
//                .setId(jti)
//                .claim("tokenVersion", tokenVersion)
//                .claim("type", tokenType)
//                .setSubject(username)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
//                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
//                .compact();
//    }
//
//    // 解析令牌
//    private Jws<Claims> parseJws(String token) {
//        return Jwts.parser()
//                .setSigningKey(getSigningKey())
//                .build()
//                .parseClaimsJws(token);
//    }
//
//    // 解析访问令牌
//    public Claims parseToken(String token) {
//        Jws<Claims> jws = parseJws(token);
//        if (!"access".equals(jws.getPayload().get("type", String.class))) {
//            throw new MalformedJwtException("访问令牌已过期");
//        }
//        return jws.getPayload();
//    }
//
//    // 解析刷新令牌
//    public Claims parseRefreshToken(String refreshToken) {
//        Jws<Claims> jws = parseJws(refreshToken);
//        if (!"refresh".equals(jws.getPayload().get("type", String.class))) {
//            throw new MalformedJwtException("刷新令牌已过期");
//        }
//        return jws.getPayload();
//    }
//
//    // 访问令牌验证方法
//    public boolean validateToken(String token) {
//        try {
//            parseToken(token);
//            return true;
//        } catch (JwtException | IllegalArgumentException ex) {
//            return false;
//        }
//    }
//
//    // 刷新令牌验证方法
//    public boolean validateRefreshToken(String refreshToken) {
//        try {
//            parseRefreshToken(refreshToken);
//            return true;
//        } catch (JwtException | IllegalArgumentException ex) {
//            return false;
//        }
//    }
//
//    // 检查是否需要刷新
//    public boolean shouldRefreshToken(String token) {
//        long remaining = getRemainingTime(token);
//        // 剩余时间小于阈值就刷新
//        return remaining < refreshThreshold;
//    }
//
//    // 获取剩余有效时间（毫秒）
//    public long getRemainingTime(String token) {
//        Claims claims = parseToken(token);
//        return claims.getExpiration().getTime() - System.currentTimeMillis();
//    }
//
//    // 从访问令牌获取JTI唯一标识
//    public String getJtiFromToken(String token) {
//        return parseToken(token).getId();
//    }
//
//    // 从刷新令牌获取JTI唯一标识
//    public String getJtiFromRefreshToken(String refreshToken) {
//        return parseRefreshToken(refreshToken).getId();
//    }
//}