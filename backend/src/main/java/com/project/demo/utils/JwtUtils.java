package com.project.demo.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * JWT组成: Header.Payload.Signature
 * 1. Header: {"alg":"HS512","typ":"JWT"}
 * 2. Payload: {"sub":"username","userId":123,"iat":1234567890,"exp":1234567890}
 * 3. Signature: HMACSHA512(base64UrlEncode(header) + "." + base64UrlEncode(payload), secretKey)
 */
@Component
public class JwtUtils {

    /** Refresh Token 闲置过期时间（秒）：24 小时内未使用则自动失效 */
    public static final long REFRESH_IDLE_SECONDS = 24 * 3600;

    /**
     * JWT签名密钥
     * 注意: 生产环境要从配置文件或环境变量读取, 不能硬编码
     * HS512算法要求密钥至少64字节
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Access Token 过期时间(毫秒) 12小时
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Refresh Token 过期时间(毫秒) 2天
     */
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 生成签名密钥
     * 使用 HS512 算法
     */
    private SecretKey getSigningKey() {
        // 确保密钥长度足够 HS512 需要至少 512 位 (64 字节)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Access Token
     *
     * @param username 用户名
     * @param userId   用户ID
     * @return JWT Token
     */
    public String generateToken(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        // 构建 Payload (Claims)
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);                    // subject: 用户名
        claims.put("userId", userId);                  // 用户ID
        claims.put("type", "access");                  // Token类型: access

        // 构建签名并生成Token
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 生成 Refresh Token (有效期更长)
     * 用途: Access Token 过期后, 用它来获取新的 Access Token
     */
    public String generateRefreshToken(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("userId", userId);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true-有效, false-无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())    // 设置签名密钥
                    .build()
                    .parseSignedClaims(token);      // 解析并验证签名
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException: Token格式错误、签名不匹配、已过期等
            // IllegalArgumentException: Token为null或空
            return false;
        }
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID 或 null
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            return (Long) userId;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 中获取签发时间 (iat)
     *
     * @param token JWT Token
     * @return 签发时间戳
     */
    public Date getIssuedAtFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getIssuedAt();
    }

    /**
     * 从 Token 中获取过期时间
     *
     * @param token JWT Token
     * @return 过期时间戳
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 从 Token 中获取Claims
     *
     * @param token JWT Token
     * @return Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 判断 Token 是否即将过期 (5分钟内过期)
     *
     * @param token JWT Token
     * @return true-即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            // 5分钟 = 300000 毫秒
            return expiration.getTime() - System.currentTimeMillis() < 300000;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Token 类型
     *
     * @param token JWT Token
     * @return access 或 refresh
     */
    public String getTokenType(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return (String) claims.get("type");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Token 默认过期时间(毫秒)
     *
     * @return 过期时间
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * 获取 Refresh Token 默认过期时间(毫秒)
     *
     * @return 过期时间
     */
    public long getRefreshExpiration() {
        return refreshExpiration;
    }
}
