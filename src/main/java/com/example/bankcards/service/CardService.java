package com.example.bankcards.service;

import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CardStatus;
import com.example.bankcards.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CardService {

    CardResponseDto saveCard(CardRequestDto card);

    CardResponseDto getCardById(UUID id);

    List<CardResponseDto> getAllCards(int pageNumber, int pageSize);

    List<CardResponseDto> getCardWaitingBlock(int pageNumber, int pageSize);

    void deleteCard(UUID id);

    void transfer(TransferRequest request);

    void blockCard(UUID cardId);

    void activateCard(UUID cardId);

    BigDecimal getCardBalance(UUID cardId);

    List<CardResponseDto> getAllCardsByUserId(UUID userId, int pageNum, int pageSize);

    List<CardResponseDto> findCardsByParameters(CardStatus status,
                                                String firstName,
                                                String lastName,
                                                String phoneNumber,
                                                String email,
                                                int pageNumber,
                                                int pageSize);

    void blockExpiredCards();

}
