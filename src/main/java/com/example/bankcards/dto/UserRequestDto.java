package com.example.bankcards.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    @NotEmpty
    @Pattern(regexp = "^[а-яА-Яa-zA-Z]+$", message = "Имя должно содержать только буквы")
    @Size(max = 50)
    private String firstName;

    @NotEmpty
    @Pattern(regexp = "^[а-яА-Яa-zA-Z]+$", message = "Фамилия должна содержать только буквы")
    @Size(max = 50)
    private String lastName;

    @NotEmpty
    @Pattern(regexp = "^((\\+7)|(8))\\d{10}$", message = "Номер телефона некорректный")
    private String phoneNumber;

    @NotEmpty
    @Email(message = "Некорректный адрес электронной почты")
    private String email;

    @NotNull
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Пароль должен содержать минимум 8 символов включая цифры, буквы в верхнем и нижем регистре, символы !@#$%^&*()")
    private String password;
}
