package com.app.game.tetris;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice // Помечаем класс как глобальный обработчик исключений
@RequiredArgsConstructor
public class WebSocketExceptionHandler {
    private final SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Формируем список ошибок
        List<String> errors = ex.getBindingResult().getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.toList());

        // Отправляем на конкретный адрес /receive/message
        messagingTemplate.convertAndSend("/receive/message",
                "Validation error: " + String.join(", ", errors));
    }

}
