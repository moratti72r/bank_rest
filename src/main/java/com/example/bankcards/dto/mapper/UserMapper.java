package com.example.bankcards.dto.mapper;

import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.dto.UserUpdateRequestDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;

import java.util.Set;

public class UserMapper {

    public static UserResponseDto userToUserResponseDto(User user) {

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getFirstName() + " " + user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .build();
    }

    public static User userRequestToUser(UserRequestDto dto, String encodePassword, Set<Role> roles) {

        return User.builder()
                .firstName(formatName(dto.getFirstName()))
                .lastName(formatName(dto.getLastName()))
                .phoneNumber(getStandartPhoneNumber(dto.getPhoneNumber()))
                .email(dto.getEmail())
                .password(encodePassword)
                .roles(roles)
                .build();

    }

    public static void userUpdateToUser(User user, UserUpdateRequestDto dto) {
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getNewPassword() != null) {
            user.setPassword(dto.getNewPassword());
        }
    }

    private static String getStandartPhoneNumber(String phone) {
        String digits = phone.replaceAll("[^\\d]", "");

        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        } else if (digits.length() == 10 && !digits.startsWith("7")) {
        }

        return "+" + digits;
    }

    private static String formatName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
