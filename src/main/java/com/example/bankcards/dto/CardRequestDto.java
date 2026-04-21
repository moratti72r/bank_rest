package com.example.bankcards.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardRequestDto {

    @NotNull
    @Pattern(regexp = "^\\d{13,19}$",
            message = "Номер карты должен содержать только цифры и быть длиной от 13 до 19 символов")
    private String cardNumber;

    @NotNull
    @Future(message = "Срок действия должен быть в будущем")
    private YearMonth expiryDate;

    @PositiveOrZero(message = "Отрицательные значения недопустимы")
    private BigDecimal balance = new BigDecimal("0.00");

    @NotEmpty
    private String userId;
}
