package com.example.bankcards.repository;

import com.example.bankcards.dto.CardStatus;
import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByHasBlockRequestTrue(Pageable pageable);

    List<Card> findAllByUserId(UUID userId, Pageable pageable);

    @Query("SELECT c " +
            "FROM Card AS c " +
            "WHERE (c.status IN ?1 OR ?1 IS NULL) " +
            "AND (LOWER(c.user.firstName) = LOWER(?2) OR ?2 IS NULL) " +
            "AND (LOWER(c.user.lastName) = LOWER(?3) OR ?3 IS NULL) " +
            "AND (c.user.phoneNumber IN ?4 OR ?4 IS NULL) " +
            "AND (LOWER(c.user.email) = LOWER(?5) OR ?5 IS NULL) ")
    List<Card> findAllByParameters(CardStatus status,
                                   String firstName,
                                   String lastName,
                                   String phoneNumber,
                                   String email,
                                   Pageable pageable);

    List<Card> findActiveCardsByExpiryDateBefore(LocalDate currentYearMonth);

    boolean existsByCardNumberHash(String cardHash);
}
