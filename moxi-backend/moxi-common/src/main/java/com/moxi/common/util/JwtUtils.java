package com.moxi.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {

    private static final String ISSUER = "moxi";
    private final SecretKey key;
    private final long accessTokenExpireMs;
    private final long refreshTokenExpireMs;

    public JwtUtils(String secret, long accessTokenExpireMs, long refreshTokenExpireMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireMs = accessTokenExpireMs;
        this.refreshTokenExpireMs = refreshTokenExpireMs;
    }

    public String generateAccessToken(Long userId, String role) {
        return buildToken(userId, role, accessTokenExpireMs, "access");
    }

    public String generateAccessToken(Long userId) {
        return generateAccessToken(userId, "user");
    }

    public String generateRefreshToken(Long userId, String role) {
        return buildToken(userId, role, refreshTokenExpireMs, "refresh");
    }

    public String generateRefreshToken(Long userId) {
        return generateRefreshToken(userId, "user");
    }

    private String buildToken(Long userId, String role, long expireMs, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("type", type);
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Long extractUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    public String extractType(String token) {
        return parseToken(token).get("type", String.class);
    }

    public long getAccessTokenExpireMs() {
        return accessTokenExpireMs;
    }
}
