package com.app.game.tetris.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Для учебного проекта часто отключают, чтобы не мучиться с токенами в HTML
                .authorizeHttpRequests(auth -> auth
                        // Разрешаем статику (картинки, звуки) и регистрацию всем
                        .requestMatchers("/img/**", "/sounds/**", "/register/**", "/html/registration.html").permitAll()
                        // Разрешаем главные страницы, но Spring будет знать, кто на них зашел
                        .requestMatchers("/", "/html/index.html", "/html/snapShot.html").permitAll()
                        // Всё остальное (например, админка или старт игры) — только после входа
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/html/hello.html", true) // Куда кинуть после логина
                        .permitAll()
                );

        return http.build();
    }

    // Здесь оставляем ТОЛЬКО статику, которую реально нет смысла даже проверять
    @Bean
    public WebSecurityCustomizer ignoringCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/img/**", "/sounds/**");
    }
}