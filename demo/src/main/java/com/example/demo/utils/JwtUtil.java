package com.example.demo.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT token
     */
    public String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)                              // ✅ 修复：setClaims
                .setIssuedAt(new Date())                        // ✅ 修复：setIssuedAt
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // ✅ 修复：setExpiration
                .signWith(getKey())                             // ✅ 正确
                .compact();
    }

    /**
     * 解析token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()                         // ✅ 修复：parserBuilder
                    .setSigningKey(getKey())                   // ✅ 修复：setSigningKey
                    .build()
                    .parseClaimsJws(token)                     // ✅ 修复：parseClaimsJws
                    .getBody();                                 // ✅ 修复：getBody
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证token有效性
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从token获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? Long.valueOf(claims.get("userId").toString()) : null;
    }
}