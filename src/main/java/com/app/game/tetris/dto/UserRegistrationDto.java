package com.app.game.tetris.dto;

import com.app.game.tetris.validation.ContainsLetter;
import com.app.game.tetris.validation.ContainsLetterDigit;
import jakarta.validation.constraints.NotBlank;


public class UserRegistrationDto {
    @NotBlank(message = "Username is mandatory")
    @ContainsLetter(message = "The username must contain at least one letter!!!!!!!") // Наша кастомная аннотация
    private String username;

    @NotBlank(message = "Password is mandatory")
    @ContainsLetterDigit(message = "The password should contain at least one letter and one digit!")
    private String password;

    @NotBlank(message = "Password confirm is mandatory")
    private String passwordConfirm;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
