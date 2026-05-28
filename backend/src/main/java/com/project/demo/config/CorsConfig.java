package com.project.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (跨域资源共享) 配置
 * 为什么需要?
 * 前后端分离项目中, 前端和后端通常运行在不同端口
 * 浏览器默认阻止跨域请求, 需要后端显式允许
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                      // 哪些路径允许跨域
                .allowedOriginPatterns("*")             // 允许的来源 (* 表示所有, 生产环境应该指定)
                .allowedMethods(
                        "GET", "POST", "PUT", "DELETE",      // 允许的HTTP方法
                        "OPTIONS"                            // 预检请求
                )
                .allowedHeaders("*")                     // 允许的请求头
                .allowCredentials(true)                  // 是否允许携带凭证(Cookie)
                .maxAge(3600);                          // 预检请求缓存时间(秒)
    }

    /**
     * CORS 配置源
     * 供 Spring Security 使用
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
