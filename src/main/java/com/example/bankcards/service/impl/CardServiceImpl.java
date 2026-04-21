package com.example.bankcards.service.impl;

import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.CardStatus;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.mapper.CardMapper;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessLogicException;
import com.example.bankcards.exception.InactiveCardException;
import com.example.bankcards.exception.NotFoundEntityException;
import com.example.bankcards.exception.UniqueValueException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.HashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.example.bankcards.exception.ExceptionMessages.*;

@Service
public class CardServiceImpl implements CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public CardResponseDto saveCard(CardRequestDto cardRequestDto) {

        String cardHash = HashUtil.hashText(cardRequestDto.getCardNumber());
        String encryptCard = encryptionService.encrypt(cardRequestDto.getCardNumber());

        if (cardRepository.existsByCardNumberHash(cardHash))
            throw new UniqueValueException("Номер карты уже существует");

        User user = userRepository.findById(UUID.fromString(cardRequestDto.getUserId())).orElseThrow(() -> new NotFoundEntityException(String.format(USER_NOT_FOUND, cardRequestDto.getUserId())));
        Card card = CardMapper.mapToCard(cardRequestDto, user, cardHash, encryptCard);
        Card saveCard = cardRepository.save(card);
        return CardMapper.mapToResponseDto(saveCard, encryptionService.decrypt(saveCard.getCardNumber()));
    }

    @Override
    public CardResponseDto getCardById(UUID id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new NotFoundEntityException(String.format(CARD_NOT_FOUND, id)));
        return CardMapper.mapToResponseDto(card, encryptionService.decrypt(card.getCardNumber()));
    }

    @Override
    public List<CardResponseDto> getCardWaitingBlock(int pageNumber, int pageSize) {
        return cardRepository.findByHasBlockRequestTrue(PageRequest.of(pageNumber / pageSize, pageSize)).stream().map(card -> CardMapper.mapToResponseDto(card, encryptionService.decrypt(card.getCardNumber()))).toList();
    }

    @Override
    public List<CardResponseDto> getAllCards(int pageNumber, int pageSize) {
        return cardRepository.findAll(PageRequest.of(pageNumber / pageSize, pageSize)).stream().map(card -> CardMapper.mapToResponseDto(card, encryptionService.decrypt(card.getCardNumber()))).toList();
    }

    @Override
    public List<CardResponseDto> getAllCardsByUserId(UUID userId, int pageNumber, int pageSize) {
        return cardRepository.findAllByUserId(userId, PageRequest.of(pageNumber / pageSize, pageSize)).stream().map(card -> CardMapper.mapToResponseDto(card, encryptionService.decrypt(card.getCardNumber()))).toList();
    }

    @Override
    public List<CardResponseDto> findCardsByParameters(CardStatus status,
                                                       String firstName,
                                                       String lastName,
                                                       String phoneNumber,
                                                       String email,
                                                       int pageNumber,
                                                       int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber / pageSize, pageSize);

        return cardRepository.findAllByParameters(
                status,
                firstName,
                lastName,
                phoneNumber,
                email,
                pageable
        ).stream().map(card -> CardMapper.mapToResponseDto(card, encryptionService.decrypt(card.getCardNumber()))).toList();
    }

    @Override
    public void deleteCard(UUID id) {
        if (!cardRepository.existsById(id)) throw new NotFoundEntityException(String.format(CARD_NOT_FOUND, id));
        cardRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void transfer(TransferRequest request) {
        Card sourceCard = cardRepository.findById(request.getSourceCardId())
                .orElseThrow(() -> new NotFoundEntityException(String.format(CARD_NOT_FOUND, request.getSourceCardId())));
        Card targetCard = cardRepository.findById(request.getTargetCardId())
                .orElseThrow(() -> new NotFoundEntityException(String.format(CARD_NOT_FOUND, request.getTargetCardId())));

        if (sourceCard.getId().equals(targetCard.getId())) {
            throw new BusinessLogicException("Нельзя перевести деньги на ту же самую карту");
        }

        if (sourceCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessLogicException("Сумма превышает баланс");
        }

        if (sourceCard.getStatus() != CardStatus.ACTIVE) {
            throw new InactiveCardException("Карта отправителя не активна");
        }

        if (targetCard.getStatus() != CardStatus.ACTIVE) {
            throw new InactiveCardException("Карта получателя не активна");
        }

        sourceCard.setBalance(sourceCard.getBalance().subtract(request.getAmount()));
        targetCard.setBalance(targetCard.getBalance().add(request.getAmount()));

        cardRepository.save(sourceCard);
        cardRepository.save(targetCard);
    }

    @Override
    @Transactional
    public void blockCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundEntityException(String.format(CARD_NOT_FOUND, cardId)));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessLogicException(String.format(CARD_ALREADY_STATUS, CardStatus.BLOCKED));
        }

        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Override
    @Transactional
    public void activateCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundEntityException(String.format(CARD_NOT_FOUND, cardId)));

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new BusinessLogicException(String.format(CARD_ALREADY_STATUS, CardStatus.ACTIVE));
        }

        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCardBalance(UUID cardId) {
        return cardRepository.findById(cardId)
                .map(Card::getBalance)
                .orElseThrow(() -> new NotFoundEntityException(String.format(CARD_NOT_FOUND, cardId)));
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    @Override
    public void blockExpiredCards() {

        LocalDate today = LocalDate.now();

        List<Card> expiredCards = cardRepository.findActiveCardsByExpiryDateBefore(today);

        if (!expiredCards.isEmpty()) {
            expiredCards.forEach(card -> {
                card.setStatus(CardStatus.EXPIRED);
            });
            cardRepository.saveAll(expiredCards);
        }
    }
}
