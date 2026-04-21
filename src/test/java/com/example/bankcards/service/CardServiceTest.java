package com.example.bankcards.service;

import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.mapper.UserMapper;
import com.example.bankcards.entity.Card;
import com.example.bankcards.dto.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CardServiceTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    private final UserRequestDto userRequestDto = createUserRequestDto();
    private User savedUser;
    private CardRequestDto validCardRequestDto;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        savedUser = userRepository.save(UserMapper.userRequestToUser(userRequestDto, "encodedPassword123", new java.util.HashSet<>()));
        validCardRequestDto = createCardRequestDto(savedUser.getId().toString());
    }

    @Test
    void saveCard_shouldPersistCardAndReturnResponse_whenValidDataProvided() {
        CardResponseDto response = cardService.saveCard(validCardRequestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getMaskedCardNumber()).isEqualTo("**** **** **** 4222");
        assertThat(response.getExpiryDate()).isEqualTo(validCardRequestDto.getExpiryDate());
        assertThat(response.getBalance()).isEqualTo(validCardRequestDto.getBalance());
        assertThat(response.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(response.getOwner().getId()).isEqualTo(savedUser.getId());

        Card savedCard = cardRepository.findById(response.getId()).orElse(null);
        assertNotNull(savedCard);
        assertEquals(HashUtil.hashText(validCardRequestDto.getCardNumber()), savedCard.getCardNumberHash());
        assertEquals(CardStatus.BLOCKED, savedCard.getStatus());
        assertTrue(savedCard.getBalance().compareTo(BigDecimal.valueOf(1000.00)) == 0);
    }

    @Test
    void saveCard_shouldThrowUniqueValueException_whenCardNumberAlreadyExists() {
        cardService.saveCard(validCardRequestDto);

        UniqueValueException exception = assertThrows(UniqueValueException.class, () -> {
            cardService.saveCard(validCardRequestDto);
        });

        assertEquals("Номер карты уже существует", exception.getMessage());
    }

    @Test
    void saveCard_shouldThrowNotFoundEntityException_whenUserDoesNotExist() {
        CardRequestDto invalidDto = createCardRequestDto(UUID.randomUUID().toString());

        NotFoundEntityException exception = assertThrows(NotFoundEntityException.class, () -> {
            cardService.saveCard(invalidDto);
        });

        assertTrue(exception.getMessage().contains("Пользователь с ID"));
    }

    @Test
    void getCardById_shouldReturnCardResponse_whenCardExists() {
        CardResponseDto savedDto = cardService.saveCard(validCardRequestDto);

        CardResponseDto foundDto = cardService.getCardById(savedDto.getId());

        assertThat(foundDto).isNotNull();
        assertThat(foundDto.getId()).isEqualTo(savedDto.getId());
        assertThat(foundDto.getMaskedCardNumber()).isEqualTo("**** **** **** 4222");
        assertThat(foundDto.getExpiryDate()).isEqualTo(validCardRequestDto.getExpiryDate());
    }

    @Test
    void getCardById_shouldThrowNotFoundEntityException_whenCardDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();

        NotFoundEntityException exception = assertThrows(NotFoundEntityException.class, () -> {
            cardService.getCardById(nonExistentId);
        });

        assertTrue(exception.getMessage().contains("Карта с ID"));
    }

    @Test
    void getAllCards_shouldReturnPagedCardsList() {
        cardService.saveCard(validCardRequestDto);

        CardRequestDto secondDto = createCardRequestDto(savedUser.getId().toString());
        secondDto.setCardNumber("5555666677778888");
        cardService.saveCard(secondDto);

        var cards = cardService.getAllCards(0, 5);

        assertThat(cards).hasSize(2);
        assertThat(cards).allMatch(card -> card.getOwner().getId().equals(savedUser.getId()));
    }

    @Test
    void getAllCardsByUserId_shouldReturnOnlyUsersCards() {
        cardService.saveCard(validCardRequestDto);

        User anotherUser = userRepository.save(User.builder()
                .firstName("Another")
                .lastName("User")
                .email("another@test.com")
                .phoneNumber("+79998887766")
                .password("pass")
                .build());

        CardRequestDto otherUserCard = createCardRequestDto(anotherUser.getId().toString());
        otherUserCard.setCardNumber("9999888877776666");
        cardService.saveCard(otherUserCard);

        var userCards = cardService.getAllCardsByUserId(savedUser.getId(), 0, 5);

        assertThat(userCards).hasSize(1);
        assertThat(userCards.get(0).getOwner().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void findCardsByParameters_shouldFilterByStatus() {
        CardResponseDto activeCard = cardService.saveCard(validCardRequestDto);
        cardService.activateCard(activeCard.getId());

        var filtered = cardService.findCardsByParameters(CardStatus.ACTIVE, null, null, null, null, 0, 5);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void findCardsByParameters_shouldFilterByOwnerInfo() {
        cardService.saveCard(validCardRequestDto);

        var filtered = cardService.findCardsByParameters(
                null,
                "TeSt",
                "UsEr",
                "+79991112233",
                "testuser@example.com",
                0,
                5
        );

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getOwner().getUsername()).isEqualTo("Test User");
    }

    @Test
    void deleteCard_shouldRemoveCardFromDatabase() {
        CardResponseDto response = cardService.saveCard(validCardRequestDto);

        assertThat(cardRepository.existsById(response.getId())).isTrue();

        cardService.deleteCard(response.getId());

        assertThat(cardRepository.existsById(response.getId())).isFalse();
    }

    @Test
    void deleteCard_shouldThrowNotFoundEntityException_whenCardDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();

        NotFoundEntityException exception = assertThrows(NotFoundEntityException.class, () -> {
            cardService.deleteCard(nonExistentId);
        });

        assertTrue(exception.getMessage().contains("Карта с ID"));
    }

    @Test
    void transfer_shouldUpdateBalances_whenValidTransfer() {
        CardResponseDto source = cardService.saveCard(validCardRequestDto);
        cardService.activateCard(source.getId());

        CardRequestDto targetDto = createCardRequestDto(savedUser.getId().toString());
        targetDto.setCardNumber("1234567812345678");
        CardResponseDto target = cardService.saveCard(targetDto);
        cardService.activateCard(target.getId());

        TransferRequest request = new TransferRequest();
        request.setSourceCardId(source.getId());
        request.setTargetCardId(target.getId());
        request.setAmount(new BigDecimal("100.00"));

        cardService.transfer(request);

        Card updatedSource = cardRepository.findById(source.getId()).orElse(null);
        Card updatedTarget = cardRepository.findById(target.getId()).orElse(null);

        assertNotNull(updatedSource);
        assertNotNull(updatedTarget);
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(updatedTarget.getBalance()).isEqualByComparingTo(new BigDecimal("1100.00"));
    }

    @Test
    void transfer_shouldThrowBusinessLogicException_whenSourceAndTargetAreSame() {
        CardResponseDto card = cardService.saveCard(validCardRequestDto);
        cardService.activateCard(card.getId());

        TransferRequest request = new TransferRequest();
        request.setSourceCardId(card.getId());
        request.setTargetCardId(card.getId());
        request.setAmount(new BigDecimal("100.00"));

        BusinessLogicException exception = assertThrows(BusinessLogicException.class, () -> {
            cardService.transfer(request);
        });

        assertEquals("Нельзя перевести деньги на ту же самую карту", exception.getMessage());
    }

    @Test
    void transfer_shouldThrowBusinessLogicException_whenInsufficientFunds() {
        CardResponseDto source = cardService.saveCard(validCardRequestDto);
        cardService.activateCard(source.getId());

        CardRequestDto targetDto = createCardRequestDto(savedUser.getId().toString());
        targetDto.setCardNumber("1234567812345678");
        CardResponseDto target = cardService.saveCard(targetDto);
        cardService.activateCard(target.getId());

        TransferRequest request = new TransferRequest();
        request.setSourceCardId(source.getId());
        request.setTargetCardId(target.getId());
        request.setAmount(new BigDecimal("1500.00"));

        BusinessLogicException exception = assertThrows(BusinessLogicException.class, () -> {
            cardService.transfer(request);
        });

        assertEquals("Сумма превышает баланс", exception.getMessage());
    }

    @Test
    void transfer_shouldThrowInactiveCardException_whenSourceCardIsNotActive() {
        CardResponseDto source = cardService.saveCard(validCardRequestDto);

        CardRequestDto targetDto = createCardRequestDto(savedUser.getId().toString());
        targetDto.setCardNumber("1234567812345678");
        CardResponseDto target = cardService.saveCard(targetDto);
        cardService.activateCard(target.getId());

        TransferRequest request = new TransferRequest();
        request.setSourceCardId(source.getId());
        request.setTargetCardId(target.getId());
        request.setAmount(new BigDecimal("100.00"));

        InactiveCardException exception = assertThrows(InactiveCardException.class, () -> {
            cardService.transfer(request);
        });

        assertEquals("Карта отправителя не активна", exception.getMessage());
    }

    @Test
    void blockCard_shouldChangeStatusToBlocked() {

        CardResponseDto card = cardService.saveCard(validCardRequestDto);
        cardService.activateCard(card.getId());

        cardService.blockCard(card.getId());

        Card updated = cardRepository.findById(card.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(CardStatus.BLOCKED, updated.getStatus());
    }

    @Test
    void blockCard_shouldThrowBusinessLogicException_whenCardAlreadyBlocked() {
        CardResponseDto card = cardService.saveCard(validCardRequestDto);

        BusinessLogicException exception = assertThrows(BusinessLogicException.class, () -> {
            cardService.blockCard(card.getId());
        });

        assertTrue(exception.getMessage().contains("Данная карта уже в статусе BLOCKED"));
    }

    @Test
    void activateCard_shouldChangeStatusToActive() {
        CardResponseDto card = cardService.saveCard(validCardRequestDto);

        cardService.activateCard(card.getId());

        Card updated = cardRepository.findById(card.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(CardStatus.ACTIVE, updated.getStatus());
    }

    @Test
    void activateCard_shouldThrowBusinessLogicException_whenCardAlreadyActive() {
        CardResponseDto card = cardService.saveCard(validCardRequestDto);
        cardService.activateCard(card.getId());

        BusinessLogicException exception = assertThrows(BusinessLogicException.class, () -> {
            cardService.activateCard(card.getId());
        });

        assertTrue(exception.getMessage().contains("Данная карта уже в статусе ACTIVE"));
    }

    @Test
    void getCardBalance_shouldReturnCurrentBalance() {
        CardResponseDto card = cardService.saveCard(validCardRequestDto);
        cardService.activateCard(card.getId());

        BigDecimal balance = cardService.getCardBalance(card.getId());

        assertThat(balance).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void getCardBalance_shouldThrowNotFoundEntityException_whenCardDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();

        NotFoundEntityException exception = assertThrows(NotFoundEntityException.class, () -> {
            cardService.getCardBalance(nonExistentId);
        });

        assertTrue(exception.getMessage().contains("Карта с ID"));
    }

    @Test
    void blockExpiredCards_shouldSetStatusToExpiredForExpiredActiveCards() {
        Card activeCard = new Card();
        activeCard.setCardNumber(encryptionService.encrypt("1111222233334444"));
        activeCard.setCardNumberHash(HashUtil.hashText("1111222233334444"));
        activeCard.setExpiryDate(LocalDate.of(2020, 1, 1));
        activeCard.setStatus(CardStatus.ACTIVE);
        activeCard.setBalance(BigDecimal.TEN);
        activeCard.setUser(savedUser);
        cardRepository.save(activeCard);

        cardService.blockExpiredCards();

        Card updated = cardRepository.findById(activeCard.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(CardStatus.EXPIRED, updated.getStatus());
    }

    private UserRequestDto createUserRequestDto() {
        UserRequestDto dto = new UserRequestDto();
        dto.setFirstName("TeSt");
        dto.setLastName("UsEr");
        dto.setEmail("testuser@example.com");
        dto.setPhoneNumber("+79991112233");
        dto.setPassword("Password123!");
        return dto;
    }

    private CardRequestDto createCardRequestDto(String userId) {
        CardRequestDto dto = new CardRequestDto();
        dto.setCardNumber("1111222233334222");
        dto.setExpiryDate(YearMonth.now().plusYears(5));
        dto.setBalance(new BigDecimal("1000.00"));
        dto.setUserId(userId);
        return dto;
    }
}