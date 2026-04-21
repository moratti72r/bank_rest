package com.example.bankcards.controller;

import com.example.bankcards.dto.UserUpdateRequestDto;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.bankcards.exception.ExceptionMessages.USER_NOT_FOUND;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class UserControllerTest {

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

    @Autowired
    private WebApplicationContext context;

    private String validToken;
    private UUID userId;
    private String email;
    private final String password = "Password123!";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

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
        userId = user.getId();

        validToken = jwtUtils.generateJwtToken(email);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void userToAdmin_shouldChangeRoleToAdmin_whenUserExistsAndAdminAuth() throws Exception {
        User admin = User.builder()
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+79991114444")
                .email("admin@example.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow()))
                .build();
        userRepository.save(admin);

        String adminToken = jwtUtils.generateJwtToken(admin.getEmail());

        mockMvc.perform(patch("/api/users/toAdmin/{id}", admin.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(admin.getId()).orElse(null);
        Assertions.assertNotNull(updatedUser);
        Assertions.assertEquals(1, updatedUser.getRoles().size());
        Assertions.assertEquals(Role.RoleName.ROLE_ADMIN, updatedUser.getRoles().iterator().next().getName());
    }

    @Test
    void userToAdmin_unauthorized_whenNoAuth() throws Exception {
        mockMvc.perform(patch("/api/users/toAdmin/{id}", userId))
                .andExpect(status().isForbidden());
    }

    @Test
    void userToAdmin_forbidden_whenUserRoleNotAdmin() throws Exception {
        mockMvc.perform(patch("/api/users/toAdmin/{id}", userId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void userToAdmin_notFound_whenUserDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        User admin = User.builder()
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+79991114444")
                .email("admin@example.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow()))
                .build();
        userRepository.save(admin);

        String adminToken = jwtUtils.generateJwtToken(admin.getEmail());

        mockMvc.perform(patch("/api/users/toAdmin/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(USER_NOT_FOUND, nonExistentId)));
    }

    @Test
    void deleteUserById_shouldDeleteUser_whenUserExistsAndAdminAuth() throws Exception {
        User admin = User.builder()
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+79991114444")
                .email("admin@example.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow()))
                .build();
        userRepository.save(admin);

        String adminToken = jwtUtils.generateJwtToken(admin.getEmail());

        Assertions.assertTrue(userRepository.existsById(admin.getId()));

        mockMvc.perform(delete("/api/users/{id}", admin.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Assertions.assertFalse(userRepository.existsById(admin.getId()));
    }

    @Test
    void deleteUserById_unauthorized_whenNoAuth() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUserById_forbidden_whenUserRoleNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUserById_notFound_whenUserDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        User admin = User.builder()
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+79991114444")
                .email("admin@example.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow()))
                .build();
        userRepository.save(admin);

        String adminToken = jwtUtils.generateJwtToken(admin.getEmail());

        mockMvc.perform(delete("/api/users/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(USER_NOT_FOUND, nonExistentId)));
    }


    @Test
    void updateUserInfo_shouldUpdateUserFields_whenValidDataProvided() throws Exception {
        String newEmail = "updateduser@example.com";
        String newPhoneNumber = "+79998884444";
        String newPassword = "NewPassword123!";

        UserUpdateRequestDto updateDto = new UserUpdateRequestDto();
        updateDto.setEmail(newEmail);
        updateDto.setPhoneNumber(newPhoneNumber);
        updateDto.setOldPassword(password);
        updateDto.setNewPassword(newPassword);

        mockMvc.perform(patch("/api/users/update")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "phoneNumber": "%s",
                                  "oldPassword": "%s",
                                  "newPassword": "%s"
                                }
                                """.formatted(newEmail, newPhoneNumber, password, newPassword)))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(userId).orElse(null);
        Assertions.assertNotNull(updatedUser);
        Assertions.assertEquals(newEmail, updatedUser.getEmail());
        Assertions.assertEquals(newPhoneNumber, updatedUser.getPhoneNumber());
        Assertions.assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()));
    }

    @Test
    void updateUserInfo_unauthorized_whenNoAuth() throws Exception {
        mockMvc.perform(patch("/api/users/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserInfo_badRequest_whenInvalidEmail() throws Exception {
        mockMvc.perform(patch("/api/users/update")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-email",
                                  "phoneNumber": "+79991112233",
                                  "oldPassword": "%s"
                                }
                                """.formatted(password)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserInfo_badRequest_whenInvalidPhoneNumber() throws Exception {
        mockMvc.perform(patch("/api/users/update")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "valid@example.com",
                                  "phoneNumber": "12345",
                                  "oldPassword": "%s"
                                }
                                """.formatted(password)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserInfo_badRequest_whenOldPasswordMissing() throws Exception {
        mockMvc.perform(patch("/api/users/update")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "valid@example.com",
                                  "phoneNumber": "+79991112233"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserInfo_businessLogicException_whenOldPasswordIncorrect() throws Exception {
        mockMvc.perform(patch("/api/users/update")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "valid@example.com",
                                  "phoneNumber": "+79991114444",
                                  "oldPassword": "WrongPass123!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Неверный пароль"));
    }

    @Test
    void updateUserInfo_uniqueValueException_whenEmailAlreadyExists() throws Exception {
        String existingEmail = "existing@example.com";
        User existingUser = User.builder()
                .firstName("Exists")
                .lastName("User")
                .email(existingEmail)
                .phoneNumber("+79995554433")
                .password(passwordEncoder.encode("Pass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow()))
                .build();
        userRepository.save(existingUser);

        mockMvc.perform(patch("/api/users/update")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "phoneNumber": "+79991117777",
                                  "oldPassword": "%s"
                                }
                                """.formatted(existingEmail, password)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Такой почтовый адрес уже существует"));
    }

    @Test
    void updateUserInfo_uniqueValueException_whenPhoneNumberAlreadyExists() throws Exception {
        String existingPhone = "+79995554433";
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Exists")
                .lastName("User")
                .email("existing@example.com")
                .phoneNumber(existingPhone)
                .password(passwordEncoder.encode("Pass123!"))
                .roles(Set.of(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow()))
                .build();
        userRepository.save(existingUser);

        mockMvc.perform(patch("/api/users/update")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "phoneNumber": "%s",
                                  "oldPassword": "%s"
                                }
                                """.formatted(existingPhone, password)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Такой номер телефона уже существует"));
    }
}