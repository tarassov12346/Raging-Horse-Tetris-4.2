package com.app.game.tetris.validation;

import com.app.game.tetris.dto.UserRegistrationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConfirmPasswordValidator implements ConstraintValidator<ConfirmPassword, UserRegistrationDto> {
    @Override
    public boolean isValid(UserRegistrationDto userRegistrationDto, ConstraintValidatorContext context) {
        return userRegistrationDto.getPassword().equals(userRegistrationDto.getPasswordConfirm());
    }
}
