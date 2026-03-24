package com.app.game.tetris.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 1. Пытаемся достать токен (через заголовок или параметр)
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    String token = (authHeader != null && authHeader.startsWith("Bearer "))
                            ? authHeader.substring(7)
                            : accessor.getFirstNativeHeader("token"); // на случай Firefox

                    if (token != null) {
                        try {
                            // 2. Извлекаем ОБЕ характеристики из JWT
                            String userId = jwtUtils.extractUserId(token);
                            String username = jwtUtils.extractUsername(token);

                            if (userId != null && username != null) {
                                // 3. Устанавливаем "комбо-принципал" через разделитель
                                String principalName = userId + ":" + username;
                                // Используем стандартный класс Spring Security
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(principalName, null, java.util.Collections.emptyList());

                                accessor.setUser(auth); // Теперь в контроллере (Authentication) principal сработает

                                System.out.println("✅ WS Auth Success: " + principalName);
                            }
                        } catch (Exception e) {
                            System.err.println("❌ JWT Error in WS: " + e.getMessage());
                        }
                    } else {
                        System.err.println("❌ No token found in CONNECT frame");
                    }
                }
                return message;
            }
        });
    }
}
