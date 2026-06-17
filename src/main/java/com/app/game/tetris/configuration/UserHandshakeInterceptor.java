package com.app.game.tetris.configuration;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class UserHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // Гейтвей уже пробросил эти заголовки!
        String userId = request.getHeaders().getFirst("X-User-Id");
        String username = request.getHeaders().getFirst("X-User-Name");

        if (userId != null) {
            attributes.put("userId", userId);
            attributes.put("username", username);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception ex) {
        // Логика после хэндшейка не требуется
    }
}
