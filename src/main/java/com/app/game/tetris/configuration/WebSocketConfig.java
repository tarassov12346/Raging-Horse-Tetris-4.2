package com.app.game.tetris.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Collections;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new UserHandshakeInterceptor()); // СВЯЗКА С ГЕЙТВЕЕМ ТУТ
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

                    // 🔥 ЗАЩИТНЫЙ БАРЬЕР: Предотвращаем NullPointerException при пустых атрибутах сессии
                    if (sessionAttributes == null) {
                        log.error("❌ [WS Auth] Отклонено: Атрибуты сессии WebSocket отсутствуют (Обход Gateway?)");
                        return message;
                    }

                    String userId = (String) sessionAttributes.get("userId");
                    String username = (String) sessionAttributes.get("username");

                    if (userId != null) {
                        String principalName = userId + ":" + (username != null ? username : "User");
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principalName, null, Collections.emptyList());
                        accessor.setUser(auth);

                        // 🔥 ЗАЩИТА ОТ PINNING: Заменяем System.out на неблокирующий SLF4J логгер
                        log.info("✅ WS Connected via Gateway: {}", principalName);
                    } else {
                        log.warn("❌ [WS Auth] Подключение отклонено: В заголовках Gateway отсутствует X-User-Id");
                    }
                }
                return message;
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
