package com.example.bankcards.service;

import com.example.bankcards.dto.*;

import java.util.UUID;


public interface UserService {

    JwtResponse login(LoginRequest loginRequest);

    void logout(String token);

    UserResponseDto register(UserRequestDto userDto);

    void userToAdmin(UUID id);

    void deleteUserById(UUID id);

    void updateUserInfo(UserUpdateRequestDto updateDto, UUID userId);
}
