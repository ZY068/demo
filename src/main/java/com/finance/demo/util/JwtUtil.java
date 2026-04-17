package com.finance.demo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    // JWT 密钥从环境变量读取，默认值仅用于开发环境
    private static final String SECRET = System.getenv("JWT_SECRET");
    // Token 过期时间（24小时）
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    private static SecretKey getKey() {
        String secret = SECRET;
        if (secret == null || secret.isEmpty()) {
            // 开发环境默认值，生产环境必须配置环境变量
            secret = "SmartReimbursementSystemSecretKey2024VeryLongAndSecureDevOnly";
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     */
    public static String generateToken(Integer userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 获取用户ID
     */
    public static Integer getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("userId", Integer.class);
    }

    /**
     * 从 Token 获取用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.getSubject();
    }
}
