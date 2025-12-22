package com.app.game.tetris.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME) // Доступна во время выполнения
@Constraint(validatedBy = ConfirmPasswordValidator.class) // Класс, который содержит логику проверки
public @interface ConfirmPassword {
    // Стандартные поля для аннотации валидации
    String message() default "The password is not confirmed!"; // Сообщение об ошибке

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
