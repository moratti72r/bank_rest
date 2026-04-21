package com.example.bankcards.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @NotNull
    private UUID sourceCardId;

    @NotNull
    private UUID targetCardId;

    @NotNull
    @Digits(integer = 7, fraction = 2, message = "Сумма не должна превышать 9999999")
    @Positive(message = "Сумму перевода должно быть больше нуля")
    private BigDecimal amount;
}
