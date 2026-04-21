package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.JwtResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.JwtUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    private String validToken;
    private String email;
    private final String password = "Password123!";

    @BeforeEach
    void setUp() throws InterruptedException {

        long rand = ThreadLocalRandom.current().nextLong(1000, 9999);
        email = "user" + rand + "@example.com";

        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .phoneNumber("+79991112233")
                .password(passwordEncoder.encode(password))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow()))
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(email, password);
        JwtResponse response = userService.login(loginRequest);
        validToken = response.getToken();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void login_shouldReturnValidJwtToken_whenCredentialsAreCorrect() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

        assertTrue(jwtUtils.validateJwtToken(validToken));
    }

    @Test
    void login_invalodEmail_return400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + "NOTVALIDEMAIL" + "\", \"password\":\"" + password + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_shouldCreateUser_andReturnResponse() throws Exception {
        String uniqueEmail = "new" + System.nanoTime() + "@example.com";

        UserRequestDto requestDto = new UserRequestDto();
        requestDto.setFirstName("NЕw");
        requestDto.setLastName("UsEr");
        requestDto.setEmail(uniqueEmail);
        requestDto.setPhoneNumber("89998887766");
        requestDto.setPassword("StrongPass123!");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "New",
                                  "lastName": "User",
                                  "email": "%s",
                                  "phoneNumber": "+79998887766",
                                  "password": "StrongPass123!"
                                }
                                """.formatted(uniqueEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("New User"))
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.phoneNumber").value("+79998887766"));

        assertTrue(userRepository.existsByEmail(uniqueEmail));
        assertTrue(passwordEncoder.matches(requestDto.getPassword(), userRepository.findByEmail(uniqueEmail).get().getPassword()));
    }

    @Test
    void registerUser_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Hacker",
                                  "lastName": "One",
                                  "email": "not-an-email",
                                  "phoneNumber": "+79991234567",
                                  "password": "ValidPass123!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Weak",
                                  "lastName": "Pass",
                                  "email": "weakpass@example.com",
                                  "phoneNumber": "+79991234567",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertNotNull(result.getResolvedException()));
    }

    @Test
    void registerUser_invalidPhoneNumber_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Bad",
                                  "lastName": "Phone",
                                  "email": "phone@example.com",
                                  "phoneNumber": "12345",
                                  "password": "ValidPass123!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_emptyFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "",
                                  "email": "",
                                  "phoneNumber": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registerUser_duplicateEmail_returns409() throws Exception {

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Again",
                                  "lastName": "User",
                                  "email": "%s",
                                  "phoneNumber": "+79992223344",
                                  "password": "ValidPass123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Такой почтовый адрес уже существует"));
    }

    @Test
    void registerUser_longStrings_returns400or201DependingOnValidation() throws Exception {
        String longString = "A".repeat(1000);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "long@example.com",
                                  "phoneNumber": "+79991112233",
                                  "password": "ValidPass123!"
                                }
                                """.formatted(longString, longString)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}