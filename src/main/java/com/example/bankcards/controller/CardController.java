package com.example.bankcards.controller;

import com.example.bankcards.dto.CardRequestDto;
import com.example.bankcards.dto.CardStatus;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@Validated
public class CardController {

    @Autowired
    private CardService cardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @ResponseStatus(HttpStatus.OK)
    public List<CardResponseDto> getAllCards(@AuthenticationPrincipal CustomUserDetails user,
                                             @RequestParam(name = "pageNum", defaultValue = "0") @PositiveOrZero int pageNum,
                                             @RequestParam(name = "pageSize", defaultValue = "10") @Positive int pageSize) {
        if (user.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))) {
            return cardService.getAllCards(pageNum, pageSize);
        } else {
            return cardService.getAllCardsByUserId(user.getId(), pageNum, pageSize);
        }
    }

    @GetMapping("/param")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public List<CardResponseDto> getAllCardsByParam(@RequestParam(name = "status", required = false) CardStatus status,
                                                    @RequestParam(name = "firstName", required = false) String firstName,
                                                    @RequestParam(name = "lastName", required = false) String lastName,
                                                    @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
                                                    @RequestParam(name = "email", required = false) String email,
                                                    @RequestParam(name = "pageNum", defaultValue = "0") @PositiveOrZero int pageNum,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") @Positive int pageSize) {
        return cardService.findCardsByParameters(status,
                firstName,
                lastName,
                phoneNumber,
                email,
                pageNum,
                pageSize);
    }

    @GetMapping("/blocklist")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public List<CardResponseDto> getCardsWaitingBlock(@RequestParam(name = "pageNum", defaultValue = "0") @PositiveOrZero int pageNum,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") @Positive int pageSize) {
        return cardService.getCardWaitingBlock(pageNum, pageSize);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponseDto createCard(@RequestBody @Valid CardRequestDto card) {
        return cardService.saveCard(card);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable("id") UUID id) {
        cardService.deleteCard(id);
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public void blockCard(@PathVariable("id") UUID id) {
        cardService.blockCard(id);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public void activateCard(@PathVariable("id") UUID id) {
        cardService.activateCard(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.ownsCard(#id)")
    @ResponseStatus(HttpStatus.OK)
    public CardResponseDto getCard(@PathVariable("id") @Param("id") UUID id) {
        return cardService.getCardById(id);
    }

    @PatchMapping("/transfer")
    @PreAuthorize("hasRole('USER') " +
            "and @userSecurityService.ownsCard(#request.sourceCardId)" +
            "and @userSecurityService.ownsCard(#request.targetCardId)")
    @ResponseStatus(HttpStatus.OK)
    public void transfer(@Valid @RequestBody @Param("request") TransferRequest request) {
        cardService.transfer(request);
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasRole('USER') and @userSecurityService.ownsCard(#id)")
    @ResponseStatus(HttpStatus.OK)
    public BigDecimal getBalance(@PathVariable("id") @Param("id") UUID id) {
        return cardService.getCardBalance(id);
    }
}
