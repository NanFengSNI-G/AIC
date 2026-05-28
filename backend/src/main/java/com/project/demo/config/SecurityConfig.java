package com.project.demo.config;

import com.project.demo.filter.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 *
 * @EnableWebSecurity: 启用 Spring Security
 * @EnableMethodSecurity: 启用方法级安全注解 (@PreAuthorize 等)
 * 核心配置:
 * 1. 密码加密器 (BCrypt)
 * 2. 安全过滤器链 (哪些请求需要认证)
 * 3. 认证提供者 (如何验证用户)
 * 4. JWT 过滤器 (处理 Token 认证)
 * 5. Session 管理策略 (无状态)
 */
@Configuration
@EnableWebSecurity           // 启用 Web 安全
@EnableMethodSecurity        // 启用方法级安全 (如 @PreAuthorize)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * ========== 密码加密器 ==========
     * BCrypt 的特点:
     * 1. 加盐防彩虹表: 每次加密结果不同
     * 2. 慢哈希: 计算成本高, 防暴力破解
     * 3. 单向哈希: 无法解密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ========== 认证提供者 ==========
     * DaoAuthenticationProvider 的认证流程:
     * 1. 根据用户名从 UserDetailsService 加载用户信息
     * 2. 使用 PasswordEncoder 验证密码
     * 3. 返回认证成功的 Authentication
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * ========== 认证管理器 ==========
     * 用于: 手动认证 (如登录接口)
     * AuthenticationManager.authenticate() 会调用 AuthenticationProvider
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * ========== 安全过滤器链 ==========
     * 这是 Spring Security 的核心配置
     * 决定: 哪些请求需要认证、哪些请求可以匿名访问
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ========== 1. 禁用 CSRF ==========
                // CSRF: 跨站请求伪造
                // 为什么禁用?
                // - 前后端分离项目, Token 在 Header 中传输, 不依赖 Cookie
                // - 移动端、API 项目通常禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // ========== 2. 配置请求授权 ==========
                .authorizeHttpRequests(auth -> auth
                        // ASYNC dispatch: SseEmitter 异步完成时 Tomcat 的二次分发，不需要重新认证
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // 公开接口: 无需认证
                        .requestMatchers(
                                "/api/auth/send-code",            // 发送验证码
                                "/api/auth/register",             // 注册(需先验证验证码)
                                "/api/auth/login",                // 用户名密码登录
                                "/api/auth/login-by-email",       // 邮箱验证码登录
                                "/api/auth/forgot-password",      // 忘记密码
                                "/api/auth/refresh-token",        // 刷新Token
                                "/api/auth/logout",               // 退出登录（Token 可能已过期）
                                "/ws/chat/**",                    // WebSocket聊天
                                "/ws/interview/**",               // WebSocket面试(文字+音频)
                                "/api/forum/sections",            // 获取板块列表
                                "/api/forum/posts",               // 获取帖子列表
                                "/api/forum/search",              // 搜索
                                "/api/images/**",                 // 图片访问（静态资源）
                                "/test/**",                        // 测试接口
                                "/api/evaluation/run"
                        ).permitAll()

                        // 管理员专用接口
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/interview/update"
                        ).hasRole("ADMIN")

                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )

                // ========== 3. 配置 Session 管理策略 ==========
                // SessionCreationPolicy.STATELESS: 无状态
                // 含义: Spring Security 不创建 HttpSession
                // 每次请求都通过 Token 认证
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ========== 4. 配置认证提供者 ==========
                .authenticationProvider(authenticationProvider())

                // ========== 5. 添加 JWT 过滤器 ==========
                // 添加到 UsernamePasswordAuthenticationFilter 之前
                // 这样在请求到达 Controller 之前, JWT 过滤器已经完成认证
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
