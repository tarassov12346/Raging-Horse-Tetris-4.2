package com.app.game.tetris.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContainsLetterDigitValidator implements ConstraintValidator<ContainsLetterDigit,String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Если поле пустое, мы не проверяем его (ответственность за @NotBlank)
        if (value == null) {
            return true;
        }
        // Сама проверка: содержит ли хотя бы одну букву и цифру
        return value.matches(".*[a-zA-Z]+.*") && value.matches(".*\\d+.*");
    }
}
