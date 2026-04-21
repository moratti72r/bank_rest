package com.example.bankcards.controller;

import com.example.bankcards.dto.UserUpdateRequestDto;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Validated
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PatchMapping("/toAdmin/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public void userToAdmin(@PathVariable("id") UUID id) {
        userService.userToAdmin(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable("id") UUID id) {
        userService.deleteUserById(id);
    }

    @PatchMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @ResponseStatus(code = HttpStatus.OK)
    public void updateUserInfo(@RequestBody @Valid UserUpdateRequestDto updateInfo,
                               @AuthenticationPrincipal CustomUserDetails user) {
        userService.updateUserInfo(updateInfo, user.getId());
    }
}
