package com.greenchain.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类（JJWT 0.11.x 版本，兼容 JDK 8/11/17）。
 *
 * P1-2 修复：密钥和过期时间改为从 application.yml 注入，不再硬编码。
 * 生产环境请务必通过环境变量 JWT_SECRET / JWT_EXPIRATION 覆盖默认值。
 */
@Component
public class JwtUtil {

    private final long expiration;
    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret:green-chain-procurement-secret-key-2024-please-change-it-later}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        // 注意：HS256 要求密钥至少 256 位（即 32 字节），这里字符串要够长
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    public String generateToken(String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId").toString();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role").toString();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 管理员端 JWT（B2 新增）：与买家端方法隔离，互不影响 ====================

    /**
     * 生成管理员 token
     * claims 含 adminId、username、role="ADMIN"，有效期 24 小时（或配置值）。
     *
     * @param adminId  管理员ID
     * @param username 管理员用户名
     * @return 签名后的 JWT 字符串
     */
    public String generateAdminToken(Long adminId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("username", username);
        claims.put("role", "ADMIN");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(adminId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从管理员 token 中解析管理员 ID
     *
     * @param token JWT 字符串
     * @return 管理员ID；token 非法或缺少 claim 时抛异常（调用方需捕获）
     */
    public Long getAdminIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        Object adminId = claims.get("adminId");
        if (adminId == null) {
            throw new IllegalArgumentException("token 中缺少 adminId");
        }
        return Long.parseLong(adminId.toString());
    }

    /**
     * 校验管理员 token：签名有效、未过期、且 role=ADMIN（防止买家端 token 冒充管理员）
     *
     * @param token JWT 字符串
     * @return true-合法的管理员 token
     */
    public boolean validateAdminToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return "ADMIN".equals(claims.get("role"));
        } catch (Exception e) {
            return false;
        }
    }
}