package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponseDto {

    private UUID id;

    private String maskedCardNumber;

    private YearMonth expiryDate;

    private CardStatus status;

    private BigDecimal balance;

    private UserResponseDto owner;

}
