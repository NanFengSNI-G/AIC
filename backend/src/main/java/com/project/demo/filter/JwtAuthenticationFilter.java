package com.project.demo.filter;

import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.utils.JwtUtils;
import com.project.demo.utils.RedisUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器

 * 职责: 每次请求时, 从请求头中提取 JWT Token 并验证

 * 工作原理:
 * 1. 继承 OncePerRequestFilter: 确保每个请求只执行一次
 * 2. 在 doFilter() 中处理认证逻辑
 * 3. Token 验证通过后，从 Redis 取 Session (UserDetails)
 * 4. 如果 Session 有效，将用户信息存入 SecurityContext
 * 5. 后续的 Spring Security 组件从 SecurityContext 获取用户信息
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 请求前缀 (从配置读取)
     */
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // ========== 1. 从请求头提取 Token ==========
        String authHeader = request.getHeader("Authorization");

        // 检查是否有 Token
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(TOKEN_PREFIX)) {
            // 没有 Token, 放行, 让后续过滤器链处理
            filterChain.doFilter(request, response);
            return;
        }

        // 提取 Token (去掉 "Bearer " 前缀)
        String token = authHeader.substring(TOKEN_PREFIX.length());

        try {
            // ========== 2. 验证 Token ==========
            if (!jwtUtils.validateToken(token)) {
                // Token 无效或已过期
                filterChain.doFilter(request, response);
                return;
            }

            // ========== 3. 从 Token 提取用户信息 ==========
            String username = jwtUtils.getUsernameFromToken(token);
            Long userId = jwtUtils.getUserIdFromToken(token);

            // ========== 4. 检查是否已经认证过 ==========
            // 避免重复设置 Authentication
            if (StringUtils.hasText(username)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                // ========== 5. 从 Redis 获取用户信息 ==========
                UserDetailsImpl userDetails = null;
                if (userId != null) {
                    userDetails = redisUtils.getUserDetails(userId);
                }

                // ========== 6. 如果用户信息不存在，拒绝访问 ==========
                if (userDetails == null) {
                    // 用户已退出登录或 Session 已失效
                    filterChain.doFilter(request, response);
                    return;
                }

                // ========== 7. 构建 Authentication 对象 ==========
                List<String> roles = userDetails.getRoles();
                List<GrantedAuthority> authorities = roles != null ? roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()) : List.of();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities
                        );

                // ========== 8. 设置认证详情 ==========
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ========== 9. 存入 SecurityContext ==========
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // ========== 10. Token 即将过期, 自动刷新 ==========
                if (jwtUtils.isTokenExpiringSoon(token)) {
                    String newToken = jwtUtils.generateToken(username, userId);
                    response.setHeader("X-New-Token", newToken);
                }
            }

        } catch (Exception e) {
            // 认证失败, 不设置 SecurityContext
            logger.error("JWT Authentication failed: " + e.getMessage());
        }

        // ========== 11. 继续过滤器链 ==========
        filterChain.doFilter(request, response);
    }
}