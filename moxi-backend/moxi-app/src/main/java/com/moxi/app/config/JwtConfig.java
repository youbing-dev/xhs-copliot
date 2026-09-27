package com.moxi.app.config;

import com.moxi.common.util.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expire:7200000}")
    private long accessTokenExpireMs;

    @Value("${jwt.refresh-token-expire:604800000}")
    private long refreshTokenExpireMs;

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(secret, accessTokenExpireMs, refreshTokenExpireMs);
    }
}
