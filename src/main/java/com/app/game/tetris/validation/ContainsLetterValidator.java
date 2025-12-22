package com.app.game.tetris.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContainsLetterValidator implements ConstraintValidator<ContainsLetter, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Если поле пустое, мы не проверяем его (ответственность за @NotBlank)
        if (value == null) {
            return true;
        }
        // Сама проверка: содержит ли хотя бы одну букву
        return value.matches(".*[a-zA-Z]+.*");
    }
}
