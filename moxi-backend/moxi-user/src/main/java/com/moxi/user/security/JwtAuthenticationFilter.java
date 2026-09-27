package com.moxi.user.security;

import com.moxi.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器
 * <p>
 * 从 Authorization 请求头中提取 Bearer 令牌，解析并校验后将用户ID和角色
 * 写入请求属性及 SecurityContext，供后续 Controller 和权限校验使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_BLACKLIST_KEY = "auth:blacklist:";
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 无需认证的白名单路径 */
    private static final List<String> SKIP_PATHS = List.of(
            "/api/v1/auth/send-code",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/billing/plans",
            "/api/v1/billing/callback/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**"
    );

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 1. 白名单路径直接放行
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 提取 Bearer 令牌
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. 校验令牌是否过期
            if (jwtUtils.isTokenExpired(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 4. 检查 Redis 黑名单
            String blacklistKey = AUTH_BLACKLIST_KEY + token;
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey))) {
                filterChain.doFilter(request, response);
                return;
            }

            // 5. 解析令牌，提取用户ID和角色
            Claims claims = jwtUtils.parseToken(token);
            Long userId = jwtUtils.extractUserId(token);
            String role = claims.get("role", String.class);

            // 6. 写入请求属性，供 Controller 使用
            request.setAttribute("userId", userId);
            request.setAttribute("role", role != null ? role : "user");

            // 7. 设置 SecurityContext 认证信息
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    role != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            : Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("JWT 认证失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 判断当前请求路径是否在白名单中
     */
    private boolean shouldSkip(String path) {
        for (String pattern : SKIP_PATHS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 Authorization 头中提取 Bearer 令牌
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
