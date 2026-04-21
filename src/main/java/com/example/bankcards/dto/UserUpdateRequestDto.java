package com.example.bankcards.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequestDto {

    @Email(message = "Некорректный адрес электронной почты")
    private String email;

    @Pattern(regexp = "^((\\+7)|(8))\\d{10}$", message = "Номер телефона некорректный")
    private String phoneNumber;

    @NotNull
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Пароль должен содержать минимум 8 символов включая цифры, буквы в верхнем и нижем регистре, символы !@#$%^&*()")
    private String oldPassword;

    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Пароль должен содержать минимум 8 символов включая цифры, буквы в верхнем и нижем регистре, символы !@#$%^&*()")
    private String newPassword;

}
