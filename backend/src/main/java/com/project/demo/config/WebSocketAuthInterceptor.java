package com.project.demo.config;

import com.project.demo.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器: 从 URL query 参数提取 token 并校验
 * 握手成功后才建立连接，userId 存入 session attributes
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        String query = request.getURI().getQuery(); // e.g., "token=xxx"
        if (!StringUtils.hasText(query) || !query.startsWith("token=")) {
            return false;
        }

        String token = query.substring(6);
        if (!jwtUtils.validateToken(token)) {
            return false;
        }

        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            return false;
        }

        // 存入 attributes，后续 Handler 和 afterConnectionClosed 可获取
        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception ex) {
        // 无需处理
    }
}
