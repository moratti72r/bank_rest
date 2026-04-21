package com.example.bankcards.service;

import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.dto.UserUpdateRequestDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessLogicException;
import com.example.bankcards.exception.NotFoundEntityException;
import com.example.bankcards.exception.UniqueValueException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static com.example.bankcards.exception.ExceptionMessages.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private final UserRequestDto requestDto = createUserRequestDto();
    private UserResponseDto responseDto;


    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        responseDto = userService.register(requestDto);
    }

    @Test
    void shouldRegisterUser_whenValidDataProvided() {

        User savedUser = userRepository.findById(responseDto.getId()).orElse(null);
        Role userRole = savedUser.getRoles().iterator().next();

        assertNotNull(savedUser);
        assertEquals("Test", savedUser.getFirstName());
        assertEquals("User", savedUser.getLastName());
        assertEquals(requestDto.getEmail(), savedUser.getEmail());
        assertEquals(requestDto.getPhoneNumber(), savedUser.getPhoneNumber());
        assertTrue(passwordEncoder.matches(requestDto.getPassword(), savedUser.getPassword()));
        assertEquals(1, savedUser.getRoles().size());
        assertEquals(Role.RoleName.ROLE_USER, userRole.getName());
    }

    @Test
    void shouldThrowUniqueValueException_whenEmailAlreadyExists() {

        UserRequestDto duplicateDto = new UserRequestDto();
        duplicateDto.setFirstName("Другой");
        duplicateDto.setLastName("Пользователь");
        duplicateDto.setEmail("testuser@example.com");
        duplicateDto.setPhoneNumber("+79991112244");
        duplicateDto.setPassword("Password123!");

        UniqueValueException exception = assertThrows(UniqueValueException.class, () -> {
            userService.register(duplicateDto);
        });

        assertEquals("Такой почтовый адрес уже существует", exception.getMessage());
    }

    @Test
    void shouldThrowUniqueValueException_whenPhoneNumberAlreadyExists() {

        UserRequestDto duplicateDto = new UserRequestDto();
        duplicateDto.setFirstName("Другой");
        duplicateDto.setLastName("Пользователь");
        duplicateDto.setEmail("another@example.com");
        duplicateDto.setPhoneNumber("+79991112233");
        duplicateDto.setPassword("Password123!");

        UniqueValueException exception = assertThrows(UniqueValueException.class, () -> {
            userService.register(duplicateDto);
        });

        assertEquals("Такой номер телефона уже существует", exception.getMessage());
    }

    @Test
    void login_ShouldReturnJwtResponse_WhenCredentialsAreValid() {
        LoginRequest loginRequest = new LoginRequest(requestDto.getEmail(), requestDto.getPassword());

        var response = userService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(requestDto.getEmail());
        assertTrue(jwtUtils.validateJwtToken(response.getToken()));
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void login_ShouldThrowBadCredentialsException_WhenEmailIsInvalid() {
        LoginRequest loginRequest = new LoginRequest("invalid@email.ru", requestDto.getPassword());


        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            userService.login(loginRequest);
        });
        assertEquals("Неверные учетные данные пользователя", exception.getMessage());
    }

    @Test
    void userToAdmin_ShouldChangeUserRoleToAdmin() {
        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Роль ROLE_ADMIN не найдена"));

        userService.userToAdmin(responseDto.getId());

        User updatedUser = userRepository.findById(responseDto.getId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getRoles()).hasSize(1);
        Role updatedRole = updatedUser.getRoles().iterator().next();
        assertThat(updatedRole.getName()).isEqualTo(Role.RoleName.ROLE_ADMIN);
    }

    @Test
    void deleteById_shouldDeleteUserByGivenId() {

        assertThat(userRepository.existsById(responseDto.getId())).isTrue();

        userService.deleteUserById(responseDto.getId());

        assertThat(userRepository.existsById(responseDto.getId())).isFalse();

    }

    @Test
    void updateUserInfo_shouldUpdateUserFields_whenValidDataProvided() {
        String newEmail = "updateduser@example.com";
        String newPhoneNumber = "+79991112255";
        String newPassword = "NewPassword123!";
        String oldPassword = requestDto.getPassword();

        UserUpdateRequestDto updateDto = new UserUpdateRequestDto();
        updateDto.setEmail(newEmail);
        updateDto.setPhoneNumber(newPhoneNumber);
        updateDto.setOldPassword(oldPassword);
        updateDto.setNewPassword(newPassword);

        userService.updateUserInfo(updateDto, responseDto.getId());

        User updatedUser = userRepository.findById(responseDto.getId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(updatedUser.getPhoneNumber()).isEqualTo(newPhoneNumber);
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()));
    }

    @Test
    void updateUserInfo_shouldKeepPasswordUnchanged_whenNewPasswordIsNull() {
        String newEmail = "updateduser@example.com";
        String newPhoneNumber = "+79991112255";
        String oldPassword = requestDto.getPassword();

        UserUpdateRequestDto updateDto = new UserUpdateRequestDto();
        updateDto.setEmail(newEmail);
        updateDto.setPhoneNumber(newPhoneNumber);
        updateDto.setOldPassword(oldPassword);
        updateDto.setNewPassword(null);

        userService.updateUserInfo(updateDto, responseDto.getId());

        User updatedUser = userRepository.findById(responseDto.getId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(updatedUser.getPhoneNumber()).isEqualTo(newPhoneNumber);
        assertTrue(passwordEncoder.matches(oldPassword, updatedUser.getPassword()));
    }

    @Test
    void updateUserInfo_shouldThrowUniqueValueException_whenEmailAlreadyExists() {
        UserRequestDto anotherUserDto = new UserRequestDto();
        anotherUserDto.setFirstName("Another");
        anotherUserDto.setLastName("User");
        anotherUserDto.setEmail("another@example.com");
        anotherUserDto.setPhoneNumber("+79991112266");
        anotherUserDto.setPassword("Password123!");

        UserResponseDto anotherUser = userService.register(anotherUserDto);

        UserUpdateRequestDto updateDto = new UserUpdateRequestDto();
        updateDto.setEmail(requestDto.getEmail());
        updateDto.setPhoneNumber("+79991112267");
        updateDto.setOldPassword(requestDto.getPassword());

        UniqueValueException exception = assertThrows(UniqueValueException.class, () -> {
            userService.updateUserInfo(updateDto, responseDto.getId());
        });

        assertEquals("Такой почтовый адрес уже существует", exception.getMessage());
    }

    @Test
    void updateUserInfo_shouldThrowUniqueValueException_whenPhoneNumberAlreadyExists() {
        UserRequestDto anotherUserDto = new UserRequestDto();
        anotherUserDto.setFirstName("Another");
        anotherUserDto.setLastName("User");
        anotherUserDto.setEmail("another@example.com");
        anotherUserDto.setPhoneNumber("+79991112266");
        anotherUserDto.setPassword("Password123!");

        UserResponseDto anotherUser = userService.register(anotherUserDto);

        UserUpdateRequestDto updateDto = new UserUpdateRequestDto();
        updateDto.setEmail(requestDto.getEmail());
        updateDto.setPhoneNumber(anotherUser.getPhoneNumber());
        updateDto.setOldPassword(requestDto.getPassword());

        UniqueValueException exception = assertThrows(UniqueValueException.class, () -> {
            userService.updateUserInfo(updateDto, responseDto.getId());
        });

        assertEquals("Такой номер телефона уже существует", exception.getMessage());
    }

    @Test
    void updateUserInfo_shouldThrowBusinessLogicException_whenOldPasswordIsIncorrect() {
        UserUpdateRequestDto updateDto = new UserUpdateRequestDto();
        updateDto.setEmail("updated@example.com");
        updateDto.setPhoneNumber("+79991112255");
        updateDto.setOldPassword("WrongPassword123!");
        updateDto.setNewPassword("NewPassword123!");

        BusinessLogicException exception = assertThrows(BusinessLogicException.class, () -> {
            userService.updateUserInfo(updateDto, responseDto.getId());
        });

        assertEquals("Неверный пароль", exception.getMessage());
    }

    @Test
    void updateUserInfo_shouldThrowNotFoundEntityException_whenUserDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();

        UserUpdateRequestDto updateDto = new UserUpdateRequestDto();
        updateDto.setEmail("updated@example.com");
        updateDto.setPhoneNumber("+79991112255");
        updateDto.setOldPassword(requestDto.getPassword());
        updateDto.setNewPassword("NewPassword123!");

        NotFoundEntityException exception = assertThrows(NotFoundEntityException.class, () -> {
            userService.updateUserInfo(updateDto, nonExistentId);
        });

        assertEquals(String.format(USER_NOT_FOUND, nonExistentId), exception.getMessage());
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
}
