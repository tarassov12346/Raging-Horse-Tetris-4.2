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

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Берем данные, которые UserHandshakeInterceptor бережно достал из заголовков Гейтвея
                    String userId = (String) accessor.getSessionAttributes().get("userId");
                    String username = (String) accessor.getSessionAttributes().get("username");

                    if (userId != null) {
                        String principalName = userId + ":" + (username != null ? username : "User");
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principalName, null, java.util.Collections.emptyList());
                        accessor.setUser(auth);
                        System.out.println("✅ WS Connected via Gateway: " + principalName);
                    } else {
                        System.err.println("❌ WS Connection refused: No headers from Gateway");
                        // Если хочешь жестко обрывать соединение:
                        // throw new org.springframework.messaging.MessagingException("Unauthorized");
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
