package com.app.game.tetris.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD}) // Аннотация применяется к полям
@Retention(RetentionPolicy.RUNTIME) // Доступна во время выполнения
@Constraint(validatedBy = ContainsLetterValidator.class) // Класс, который содержит логику проверки
public @interface ContainsLetter {
    // Стандартные поля для аннотации валидации
    String message() default "The field must contain at least one letter"; // Сообщение об ошибке

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
