package com.ragsystem.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Autowired
    private JwtProperties jwtProperties;

    // 1. 统一获取密钥的方法
    private SecretKey getSecretKey() {
        // 确保配置文件中的 secret 足够长（32字符以上）
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    private static final long DEFAULT_EXPIRE_TIME = 2 * 60 * 60 * 1000L;

    public long getExpirationMillis() {
        Long configured = jwtProperties.getExpiration();
        return configured != null && configured > 0 ? configured : DEFAULT_EXPIRE_TIME;
    }

    // 2. 生成 Token
    public String createToken(Long userId, String username, String role) {
        Date expireDate = new Date(System.currentTimeMillis() + getExpirationMillis());
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .setExpiration(expireDate)
                .signWith(getSecretKey())
                .compact();
    }

    // 3. 解析 token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSecretKey()) // 修改这里：保持一致
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage()); // 加行日志，方便你在控制台看报错
            return false;
        }
    }
}