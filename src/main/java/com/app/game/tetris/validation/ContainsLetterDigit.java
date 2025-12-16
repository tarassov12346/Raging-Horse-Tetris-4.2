package com.app.game.tetris.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD}) // Аннотация применяется к полям
@Retention(RetentionPolicy.RUNTIME) // Доступна во время выполнения
@Constraint(validatedBy = ContainsLetterDigitValidator.class) // Класс, который содержит логику проверки
public @interface ContainsLetterDigit {
    String message() default "The password should contain at least one letter and one digit!"; // Сообщение об ошибке

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
