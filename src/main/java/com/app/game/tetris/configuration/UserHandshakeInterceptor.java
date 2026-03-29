package com.app.game.tetris.configuration;

public class UserHandshakeInterceptor implements org.springframework.web.socket.server.HandshakeInterceptor{
    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   java.util.Map<String, Object> attributes) {

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
    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               org.springframework.web.socket.WebSocketHandler wsHandler, Exception ex) {}
}
