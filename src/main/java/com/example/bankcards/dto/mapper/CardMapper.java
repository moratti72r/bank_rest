package com.example.bankcards.dto.mapper;

import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;

import java.time.YearMonth;

public class CardMapper {

    public static CardResponseDto mapToResponseDto(Card card, String decryptCardNumber) {
        UserResponseDto userDto = UserMapper.userToUserResponseDto(card.getUser());
        return CardResponseDto.builder()
                .id(card.getId())
                .maskedCardNumber(getMaskedCardNumber(decryptCardNumber))
                .expiryDate(YearMonth.from(card.getExpiryDate()))
                .status(card.getStatus())
                .balance(card.getBalance())
                .owner(userDto)
                .build();
    }

    public static Card mapToCard(CardRequestDto cardDto, User user, String cardHash, String encryptCard) {
        return Card.builder()
                .cardNumber(encryptCard)
                .cardNumberHash(cardHash)
                .expiryDate(cardDto.getExpiryDate().atDay(1))
                .balance(cardDto.getBalance())
                .user(user)
                .build();
    }

    private static String getMaskedCardNumber(String cardNumber) {

        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
