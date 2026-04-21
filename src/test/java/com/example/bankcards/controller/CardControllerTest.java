package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.HashUtil;
import com.example.bankcards.util.JwtUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private CardService cardService;

    private String adminToken;
    private String userToken;
    private UUID userId;
    private UUID adminId;
    private UUID cardId;
    private final String userPassword = "Password123!";
    private final String adminPassword = "AdminPass123!";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("testuser@example.com")
                .phoneNumber("+79991112233")
                .password(passwordEncoder.encode(userPassword))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow()))
                .build();
        userRepository.save(user);
        userId = user.getId();
        userToken = jwtUtils.generateJwtToken(user.getEmail());

        User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .phoneNumber("+79991114444")
                .password(passwordEncoder.encode(adminPassword))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow()))
                .build();
        userRepository.save(admin);
        adminId = admin.getId();
        adminToken = jwtUtils.generateJwtToken(admin.getEmail());

        String cardNumber = "4111111111111111";
        String encryptedCard = encryptionService.encrypt(cardNumber);
        String cardHash = HashUtil.hashText(cardNumber);

        Card card = Card.builder()
                .cardNumber(encryptedCard)
                .cardNumberHash(cardHash)
                .expiryDate(YearMonth.now().plusYears(2).atDay(1))
                .balance(BigDecimal.valueOf(1000.00))
                .status(CardStatus.ACTIVE)
                .user(user)
                .build();
        cardRepository.save(card);
        cardId = card.getId();
    }

    @AfterEach
    void tearDown() {
        cardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createCard_unauthorized_whenNoAuth() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_forbidden_whenUserNotAdmin() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_badRequest_whenInvalidCardNumber() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "1234",
                                  "expiryDate": "2028-12",
                                  "balance": 500,
                                  "userId": "%s"
                                }
                                """.formatted(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_conflict_whenCardNumberExists() throws Exception {
        CardRequestDto existing = new CardRequestDto();
        existing.setCardNumber("4111111111111111");
        existing.setExpiryDate(YearMonth.now().plusYears(2));
        existing.setBalance(BigDecimal.valueOf(100));
        existing.setUserId(userId.toString());

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "4111111111111111",
                                  "expiryDate": "2026-12",
                                  "balance": 100,
                                  "userId": "%s"
                                }
                                """.formatted(userId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Номер карты уже существует"));
    }

    @Test
    void getCardById_shouldReturnCard_whenExistsAndAuthorized() throws Exception {
        mockMvc.perform(get("/api/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 1111"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.owner.username").value("Test User"));
    }

    @Test
    void getCardById_forbidden_whenUserDoesNotOwnCard() throws Exception {
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Other")
                .lastName("User")
                .email("other@example.com")
                .phoneNumber("+79995556677")
                .password(passwordEncoder.encode("Pass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow()))
                .build();
        userRepository.save(otherUser);

        String otherToken = jwtUtils.generateJwtToken(otherUser.getEmail());

        mockMvc.perform(get("/api/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCardById_ok_whenAdminRequestsAnyCard() throws Exception {
        mockMvc.perform(get("/api/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()));
    }

    @Test
    void deleteCard_shouldDelete_whenExistsAndAdminAuth() throws Exception {
        assertTrue(cardRepository.existsById(cardId));

        mockMvc.perform(delete("/api/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Assertions.assertFalse(cardRepository.existsById(cardId));
    }

    @Test
    void deleteCard_forbidden_whenUserNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void blockCard_shouldChangeStatusToBlocked_whenValidAndAdminAuth() throws Exception {
        mockMvc.perform(patch("/api/cards/{id}/block", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Card updated = cardRepository.findById(cardId).orElse(null);
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(CardStatus.BLOCKED, updated.getStatus());
    }

    @Test
    void blockCard_conflict_whenAlreadyBlocked() throws Exception {
        Card card = cardRepository.findById(cardId).orElseThrow();
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);

        mockMvc.perform(patch("/api/cards/{id}/block", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Данная карта уже в статусе BLOCKED"));
    }

    @Test
    void activateCard_shouldActivate_whenValidAndAdminAuth() throws Exception {
        Card card = cardRepository.findById(cardId).orElseThrow();
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);

        mockMvc.perform(patch("/api/cards/{id}/activate", cardId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Card updated = cardRepository.findById(cardId).orElse(null);
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(CardStatus.ACTIVE, updated.getStatus());
    }

    @Test
    void transfer_shouldTransferMoney_whenValidAndUserOwnsCards() throws Exception {
        String card2Number = "4333333333333333";
        Card card2 = Card.builder()
                .cardNumber(encryptionService.encrypt(card2Number))
                .cardNumberHash(HashUtil.hashText(card2Number))
                .expiryDate(YearMonth.now().plusYears(2).atDay(1))
                .balance(BigDecimal.valueOf(500))
                .status(CardStatus.ACTIVE)
                .user(userRepository.findById(userId).orElseThrow())
                .build();
        cardRepository.save(card2);

        mockMvc.perform(patch("/api/cards/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceCardId": "%s",
                                  "targetCardId": "%s",
                                  "amount": 200
                                }
                                """.formatted(cardId, card2.getId())))
                .andExpect(status().isOk());

        Card source = cardRepository.findById(cardId).orElse(null);
        Card target = cardRepository.findById(card2.getId()).orElse(null);

        Assertions.assertEquals(800.00, source.getBalance().doubleValue());
        Assertions.assertEquals(700.00, target.getBalance().doubleValue());
    }

    @Test
    void transfer_forbidden_whenUserDoesNotOwnCard() throws Exception {
        User otherUser = User.builder()
                .firstName("Other")
                .lastName("User")
                .email("other@example.com")
                .phoneNumber("+79995556677")
                .password(passwordEncoder.encode("Pass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow()))
                .build();
        userRepository.save(otherUser);

        String card2Number = "4333333333333333";
        Card card2 = Card.builder()
                .cardNumber(encryptionService.encrypt(card2Number))
                .cardNumberHash(HashUtil.hashText(card2Number))
                .expiryDate(YearMonth.now().plusYears(2).atDay(1))
                .balance(BigDecimal.valueOf(500))
                .status(CardStatus.ACTIVE)
                .user(otherUser)
                .build();
        cardRepository.save(card2);

        mockMvc.perform(patch("/api/cards/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceCardId": "%s",
                                  "targetCardId": "%s",
                                  "amount": 100
                                }
                                """.formatted(cardId, card2.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCards_shouldReturnUserCards_whenUser() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllCards_shouldReturnAllCards_whenAdmin() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNotEmpty());
    }

    @Test
    void getBalance_shouldReturnBalance_whenCardExistsAndUserOwnsIt() throws Exception {
        mockMvc.perform(get("/api/cards/{id}/balance", cardId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));
    }

    @Test
    void getBalance_forbidden_whenUserDoesNotOwnCard() throws Exception {
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Other")
                .lastName("User")
                .email("other@example.com")
                .phoneNumber("+79995556677")
                .password(passwordEncoder.encode("Pass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow()))
                .build();
        userRepository.save(otherUser);

        String otherToken = jwtUtils.generateJwtToken(otherUser.getEmail());

        mockMvc.perform(get("/api/cards/{id}/balance", cardId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}