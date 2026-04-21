package com.example.bankcards.service.impl;

import com.example.bankcards.dto.*;
import com.example.bankcards.dto.mapper.UserMapper;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessLogicException;
import com.example.bankcards.exception.NotFoundEntityException;
import com.example.bankcards.exception.UniqueValueException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.example.bankcards.exception.ExceptionMessages.USER_NOT_FOUND;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundEntityException("Пользователь с почтой + " + email + " не найден"));
        Role role = user.getRoles().iterator().next();
        String jwt = jwtUtils.generateJwtToken(email);

        return new JwtResponse(jwt, email, role.getName().name());

    }

    @Override
    public void logout(String token) {
        jwtUtils.blacklistToken(token);
    }

    @Override
    public UserResponseDto register(UserRequestDto userDto) {

        String encodedPassowrd = passwordEncoder.encode(userDto.getPassword());
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new NotFoundEntityException("Роль + " + Role.RoleName.ROLE_USER + " не найдена"));

        User user = UserMapper.userRequestToUser(userDto, encodedPassowrd, Set.of(userRole));

        if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            throw new UniqueValueException("Такой номер телефона уже существует");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UniqueValueException("Такой почтовый адрес уже существует");
        }
        return UserMapper.userToUserResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void userToAdmin(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundEntityException(String.format(USER_NOT_FOUND, id)));
        Role role = roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow(() -> new NotFoundEntityException("Роль отсутствует"));
        user.setRoles(new HashSet<>(Set.of(role)));
        userRepository.save(user);
    }

    @Override
    public void deleteUserById(UUID id) {
        if (!userRepository.existsById(id)) throw new NotFoundEntityException(String.format(USER_NOT_FOUND, id));
        userRepository.deleteById(id);
    }

    @Override
    public void updateUserInfo(UserUpdateRequestDto updateDto, UUID userId) {
        if (userRepository.existsByPhoneNumber(updateDto.getPhoneNumber()))
            throw new UniqueValueException("Такой номер телефона уже существует");
        if (userRepository.existsByEmail(updateDto.getEmail()))
            throw new UniqueValueException("Такой почтовый адрес уже существует");

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundEntityException(String.format(USER_NOT_FOUND, userId)));

        if (!passwordEncoder.matches(updateDto.getOldPassword(), user.getPassword()))
            throw new BusinessLogicException("Неверный пароль");

        updateDto.setOldPassword(passwordEncoder.encode(updateDto.getOldPassword()));
        updateDto.setNewPassword(updateDto.getNewPassword() != null ? passwordEncoder.encode(updateDto.getNewPassword()) : null);
        UserMapper.userUpdateToUser(user, updateDto);
        userRepository.save(user);
    }
}
