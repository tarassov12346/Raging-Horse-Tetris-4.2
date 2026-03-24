package com.app.game.tetris.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtils {

    private final String jwtSecret = "mySecretKeyForTetrisProjectWhichMustBeAtLeast32CharactersLong!";
    private final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

    // --- МЕТОДЫ ИЗВЛЕЧЕНИЯ ---

    // 1. Извлекаем ID (который мы положили в claims)
    public String extractUserId(String token) {
        return String.valueOf(extractAllClaims(token).get("userId"));
    }

    // 2. Извлекаем ИМЯ (которое лежит в Subject)
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
